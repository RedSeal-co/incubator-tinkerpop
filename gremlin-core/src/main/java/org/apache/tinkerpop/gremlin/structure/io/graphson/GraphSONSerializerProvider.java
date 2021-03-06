/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.structure.io.graphson;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Implementation of the {@code DefaultSerializerProvider} for Jackson that users the {@code ToStringSerializer} for
 * unknown types.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
final class GraphSONSerializerProvider extends DefaultSerializerProvider {
    private static final long serialVersionUID = 1L;
    private static final ToStringSerializer toStringSerializer = new ToStringSerializer();

    public GraphSONSerializerProvider() {
        super();
    }

    protected GraphSONSerializerProvider(final SerializerProvider src,
                                         final SerializationConfig config, final SerializerFactory f) {
        super(src, config, f);
    }

    @Override
    public JsonSerializer<Object> getUnknownTypeSerializer(final Class<?> aClass) {
        return toStringSerializer;
    }

    @Override
    public GraphSONSerializerProvider createInstance(final SerializationConfig config,
                                                     final SerializerFactory jsf) {
        return new GraphSONSerializerProvider(this, config, jsf);
    }
}
