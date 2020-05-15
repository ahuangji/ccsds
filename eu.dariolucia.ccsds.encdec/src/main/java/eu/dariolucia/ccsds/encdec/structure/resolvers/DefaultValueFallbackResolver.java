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

package eu.dariolucia.ccsds.encdec.structure.resolvers;

import eu.dariolucia.ccsds.encdec.definition.EncodedParameter;
import eu.dariolucia.ccsds.encdec.definition.PacketDefinition;
import eu.dariolucia.ccsds.encdec.structure.EncodingException;
import eu.dariolucia.ccsds.encdec.structure.IEncodeResolver;
import eu.dariolucia.ccsds.encdec.structure.PathLocation;
import eu.dariolucia.ccsds.encdec.time.AbsoluteTimeDescriptor;
import eu.dariolucia.ccsds.encdec.time.RelativeTimeDescriptor;
import eu.dariolucia.ccsds.encdec.value.BitString;
import eu.dariolucia.ccsds.encdec.value.StringUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

/**
 * A resolver that returns the value as provided by the delegate resolver. If not present (null) or with exception, then
 * the call provides the default value as per definition.
 * If not present or the value cannot be parsed, an exception is thrown.
 */
public class DefaultValueFallbackResolver implements IEncodeResolver {

    private final IEncodeResolver delegate;

    protected PacketDefinition currentDefinition;

    public DefaultValueFallbackResolver(IEncodeResolver delegate) {
        if(delegate == null) {
            throw new NullPointerException("Delegate resolver must be provided");
        }
        this.delegate = delegate;
    }

    /**
     * Generic function used to forward the call to the delegate object or retrieve the default value.
     *
     * @param parameter the encoded parameter
     * @param location the location of the encoded parameter
     * @param delegateFunction the delegate function
     * @param parseFunction the parse function
     * @param <T> the type of the result
     * @return the value object
     */
    private <T> T retrieveValue(EncodedParameter parameter, PathLocation location,
                                EncodingBiFunction<EncodedParameter, PathLocation, T> delegateFunction, Function<String, T> parseFunction) throws EncodingException {
        T toReturn = null;
        try {
            toReturn = delegateFunction.apply(parameter, location);
        } catch (Exception e) {
            // Ignore and try with the default
        }

        if(toReturn == null) {
            // Go for default
            String value = parameter.getValue();
            if (value == null) {
                throw new EncodingException("No default definition-value retrievable for " + location + ", default value not set");
            } else {
                try {
                    toReturn = parseFunction.apply(value);
                } catch (Exception e) {
                    throw new EncodingException("No parsable default definition-value retrievable for " + location, e);
                }
            }
        }

        return toReturn;
    }

    /**
     * Generic function used to forward the call to the delegate object or retrieve the default value.
     *
     * @param parameter the encoded parameter
     * @param location the location of the encoded parameter
     * @param length the max length of the value
     * @param delegateFunction the delegate function
     * @param parseFunction the parse function
     * @param <T> the type of the result
     * @return the value object
     */
    private <T> T retrieveValue(EncodedParameter parameter, PathLocation location, int length,
                                EncodingTriFunction<EncodedParameter, PathLocation, Integer, T> delegateFunction, Function<String, T> parseFunction) throws EncodingException {
        T toReturn = null;
        try {
            toReturn = delegateFunction.apply(parameter, location, length);
        } catch (Exception e) {
            // Ignore and try with the default
        }

        if(toReturn == null) {
            // Go for default
            String value = parameter.getValue();
            if (value == null) {
                throw new EncodingException("No default definition-value retrievable for " + location + ", default value not set");
            } else {
                try {
                    toReturn = parseFunction.apply(value);
                } catch (Exception e) {
                    throw new EncodingException("No parsable default definition-value retrievable for " + location, e);
                }
            }
        }

        return toReturn;
    }

    @Override
    public boolean getBooleanValue(EncodedParameter parameter, PathLocation location) throws EncodingException {
        return retrieveValue(parameter, location, delegate::getBooleanValue, Boolean::parseBoolean);
    }

    @Override
    public int getEnumerationValue(EncodedParameter parameter, PathLocation location) throws EncodingException {
        return retrieveValue(parameter, location, delegate::getEnumerationValue, Integer::parseInt);
    }

    @Override
    public long getSignedIntegerValue(EncodedParameter parameter, PathLocation location) throws EncodingException {
        return retrieveValue(parameter, location, delegate::getSignedIntegerValue, Long::parseLong);
    }

    @Override
    public long getUnsignedIntegerValue(EncodedParameter parameter, PathLocation location) throws EncodingException {
        return retrieveValue(parameter, location, delegate::getUnsignedIntegerValue, Long::parseLong);
    }

    @Override
    public double getRealValue(EncodedParameter parameter, PathLocation location) throws EncodingException {
        return retrieveValue(parameter, location, delegate::getRealValue, Double::parseDouble);
    }

    @Override
    public Instant getAbsoluteTimeValue(EncodedParameter parameter, PathLocation location) throws EncodingException {
        return retrieveValue(parameter, location, delegate::getAbsoluteTimeValue, Instant::parse);
    }

    @Override
    public Duration getRelativeTimeValue(EncodedParameter parameter, PathLocation location) throws EncodingException {
        return retrieveValue(parameter, location, delegate::getRelativeTimeValue, Duration::parse);
    }

    @Override
    public BitString getBitStringValue(EncodedParameter parameter, PathLocation location, int maxBitlength) throws EncodingException {
        return retrieveValue(parameter, location, maxBitlength, delegate::getBitStringValue, BitString::parseBitString);
    }

    @Override
    public byte[] getOctetStringValue(EncodedParameter parameter, PathLocation location, int maxByteLength) throws EncodingException {
        return retrieveValue(parameter, location, maxByteLength, delegate::getOctetStringValue, StringUtil::toByteArray);
    }

    @Override
    public String getCharacterStringValue(EncodedParameter parameter, PathLocation location, int maxStringLength) throws EncodingException {
        return retrieveValue(parameter, location, maxStringLength, delegate::getCharacterStringValue, Function.identity());
    }

    @Override
    public Object getExtensionValue(EncodedParameter parameter, PathLocation location) throws EncodingException {
        return delegate.getExtensionValue(parameter, location);
    }

    @Override
    public AbsoluteTimeDescriptor getAbsoluteTimeDescriptor(EncodedParameter parameter, PathLocation location, Instant value) throws EncodingException {
        return delegate.getAbsoluteTimeDescriptor(parameter, location, value);
    }

    @Override
    public RelativeTimeDescriptor getRelativeTimeDescriptor(EncodedParameter parameter, PathLocation location, Duration value) throws EncodingException {
        return delegate.getRelativeTimeDescriptor(parameter, location, value);
    }

    @Override
    public void startPacketEncoding(PacketDefinition pd) {
        this.currentDefinition = pd;
        delegate.startPacketEncoding(pd);
    }

    @Override
    public void endPacketEncoding() {
        this.currentDefinition = null;
        delegate.endPacketEncoding();
    }

    @FunctionalInterface
    private interface EncodingTriFunction<T, U, K, R> {
        R apply(T t, U u, K k) throws EncodingException;
    }

    @FunctionalInterface
    private interface EncodingBiFunction<T, U, R> {
        R apply(T t, U u) throws EncodingException;
    }
}
