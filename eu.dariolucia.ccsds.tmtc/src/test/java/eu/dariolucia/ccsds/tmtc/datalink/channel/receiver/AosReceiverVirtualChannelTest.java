/*
 *   Copyright (c) 2019 Dario Lucia (https://www.dariolucia.eu)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package eu.dariolucia.ccsds.tmtc.datalink.channel.receiver;

import eu.dariolucia.ccsds.tmtc.algorithm.ReedSolomonAlgorithm;
import eu.dariolucia.ccsds.tmtc.coding.decoder.ReedSolomonDecoder;
import eu.dariolucia.ccsds.tmtc.coding.decoder.TmAsmDecoder;
import eu.dariolucia.ccsds.tmtc.coding.reader.LineHexDumpChannelReader;
import eu.dariolucia.ccsds.tmtc.datalink.channel.VirtualChannelAccessMode;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AosTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TmTransferFrame;
import eu.dariolucia.ccsds.tmtc.util.StreamUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AosReceiverVirtualChannelTest {

    private static String FILE_TM1 = "dumpFile_aos_1.hex";
    private static String FILE_TM1_GAP = "dumpFile_aos_1_gap.hex";


    @Test
    public void testAosVc0SpacePacket() throws IOException {
        // Create a virtual channel for VC0
        AosReceiverVirtualChannel vc0 = new AosReceiverVirtualChannel(0, VirtualChannelAccessMode.Packet, true);
        // Subscribe a packet collector
        List<byte[]> goodPackets = new CopyOnWriteArrayList<>();
        List<byte[]> badPackets = new CopyOnWriteArrayList<>();
        final AtomicInteger frameCounter = new AtomicInteger(0);
        vc0.register(new IVirtualChannelReceiverOutput() {
            @Override
            public void transferFrameReceived(AbstractReceiverVirtualChannel vc, AbstractTransferFrame receivedFrame) {
                frameCounter.incrementAndGet();
            }

            @Override
            public void spacePacketExtracted(AbstractReceiverVirtualChannel vc, AbstractTransferFrame lastFrame, byte[] packet, boolean qualityIndicator) {
                if(qualityIndicator) {
                    goodPackets.add(packet);
                } else {
                    badPackets.add(packet);
                }
            }

            @Override
            public void dataExtracted(AbstractReceiverVirtualChannel vc, AbstractTransferFrame frame, byte[] data) {

            }

            @Override
            public void bitstreamExtracted(AbstractReceiverVirtualChannel vc, AbstractTransferFrame frame, byte[] data, int numBits) {

            }

            @Override
            public void gapDetected(AbstractReceiverVirtualChannel vc, int expectedVc, int receivedVc, int missingFrames) {

            }
        });
        // Build the reader
        LineHexDumpChannelReader reader = new LineHexDumpChannelReader(this.getClass().getClassLoader().getResourceAsStream(FILE_TM1));
        // Use stream approach: no need for decoder
        StreamUtil.from(reader) // Reads the frames, correctly segmented
                .map(new TmAsmDecoder()) // Remove ASM
                .map(new ReedSolomonDecoder(ReedSolomonAlgorithm.TM_255_223)) // Remove R-S codeblock
                .map(AosTransferFrame.decodingFunction(false, 0, AosTransferFrame.UserDataType.M_PDU, true, false)) // Convert to AOS frame
                .filter(o -> o.getVirtualChannelId() == 0) // Filter out VCs not equal to 0
                .forEach(vc0);
        // Check the number of VC0 frames
        assertEquals(11, frameCounter.get());
        // Check the list of packets
        assertEquals(23, goodPackets.size());
        assertEquals(0, badPackets.size());
    }

    @Test
    public void testAosVc0Gap() throws IOException {
        // Create a virtual channel for VC0
        AosReceiverVirtualChannel vc0 = new AosReceiverVirtualChannel(0, VirtualChannelAccessMode.Packet, true);
        // Subscribe a packet collector
        List<byte[]> goodPackets = new CopyOnWriteArrayList<>();
        List<byte[]> badPackets = new CopyOnWriteArrayList<>();
        final AtomicInteger frameCounter = new AtomicInteger(0);
        AtomicBoolean gapDetected = new AtomicBoolean(false);
        vc0.register(new IVirtualChannelReceiverOutput() {
            @Override
            public void transferFrameReceived(AbstractReceiverVirtualChannel vc, AbstractTransferFrame receivedFrame) {
                frameCounter.incrementAndGet();
            }

            @Override
            public void spacePacketExtracted(AbstractReceiverVirtualChannel vc, AbstractTransferFrame lastFrame, byte[] packet, boolean qualityIndicator) {
                if(qualityIndicator) {
                    goodPackets.add(packet);
                } else {
                    badPackets.add(packet);
                }
            }

            @Override
            public void dataExtracted(AbstractReceiverVirtualChannel vc, AbstractTransferFrame frame, byte[] data) {

            }

            @Override
            public void bitstreamExtracted(AbstractReceiverVirtualChannel vc, AbstractTransferFrame frame, byte[] data, int numBits) {

            }

            @Override
            public void gapDetected(AbstractReceiverVirtualChannel vc, int expectedVc, int receivedVc, int missingFrames) {
                gapDetected.set(true);
            }
        });
        // Build the reader
        LineHexDumpChannelReader reader = new LineHexDumpChannelReader(this.getClass().getClassLoader().getResourceAsStream(FILE_TM1_GAP));
        // Use stream approach: no need for decoder
        StreamUtil.from(reader) // Reads the frames, correctly segmented
                .map(new TmAsmDecoder()) // Remove ASM
                .map(new ReedSolomonDecoder(ReedSolomonAlgorithm.TM_255_223)) // Remove R-S codeblock
                .map(AosTransferFrame.decodingFunction(false, 0, AosTransferFrame.UserDataType.M_PDU, true, false)) // Convert to AOS frame
                .filter(o -> o.getVirtualChannelId() == 0) // Filter out VCs not equal to 0
                .forEach(vc0);
        // Check the number of VC0 frames
        assertEquals(10, frameCounter.get());
        // Check the list of packets
        assertEquals(19, goodPackets.size());
        assertEquals(1, badPackets.size());
        assertTrue(gapDetected.get());
    }
}