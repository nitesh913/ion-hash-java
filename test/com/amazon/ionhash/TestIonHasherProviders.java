/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazon.ionhash;

import com.amazon.ion.IonSexp;
import com.amazon.ion.IonSystem;
import com.amazon.ion.system.IonSystemBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * FOR TEST PURPOSES ONLY!!!
 *
 * Each call to hash() simply returns the input byte[], unmodified.
 * Tracks all hash() calls so IonHashRunner can verify correct behavior
 * of IonhashingReaderImpl.
 *
 * @see IonHashRunner
 */
class TestIonHasherProviders {
    abstract static class TestIonHasherProvider implements IonHasherProvider {
        private final IonSystem ION = IonSystemBuilder.standard().build();
        private final IonSexp hashLog = ION.newEmptySexp();

        void addHashToLog(String method, byte[] hash) {
            IonSexp node = ION.newSexp();
            node.addTypeAnnotation(method);
            for (byte b : hash) {
                node.add(ION.newInt(b & 0xFF));
            }
            hashLog.add(node);
        }

        IonSexp getHashLog() {
            return hashLog;
        }
    }

    static TestIonHasherProvider getInstance(final String algorithm) {
        switch (algorithm) {
            case "identity":
                return new TestIonHasherProvider() {
                    @Override
                    public IonHasher newHasher() {
                        return new IonHasher() {
                            private ByteArrayOutputStream baos = new ByteArrayOutputStream();

                            @Override
                            public void update(byte[] bytes) {
                                addHashToLog("update", bytes);
                                try {
                                    baos.write(bytes);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            @Override
                            public byte[] digest() {
                                byte[] bytes = baos.toByteArray();
                                baos.reset();
                                addHashToLog("digest", bytes);
                                return bytes;
                            }
                        };
                    }
                };

            default:
                return new TestIonHasherProvider() {
                    @Override
                    public IonHasher newHasher() {
                        try {
                            return new IonHasher() {
                                private MessageDigest md = MessageDigest.getInstance(algorithm);

                                @Override
                                public void update(byte[] bytes) {
                                    md.update(bytes);
                                }

                                @Override
                                public byte[] digest() {
                                    byte[] hash = md.digest();
                                    addHashToLog("digest", hash);
                                    return hash;
                                }
                            };
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
        }
    }
}
