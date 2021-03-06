/*
 * Copyright 2018-2019 Dario Lucia (https://www.dariolucia.eu)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.dariolucia.ccsds.inspector.view.charts;

import java.time.Instant;

public class BitrateSample {
    private final Instant time;
    private final long bps;

    public BitrateSample(Instant time, long bps) {
        this.time = time;
        this.bps = bps;
    }

    public Instant getTime() {
        return time;
    }

    public long getBps() {
        return bps;
    }
}
