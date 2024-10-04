/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.camel.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.SchemaResolver;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.dataformat.ProtobufDataFormat;
import org.apache.camel.model.dataformat.ProtobufLibrary;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "karaf-camel-jackson-protobuf-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelJacksonProtobufRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    public static final String PROTOBUF_BEAN = "protobufBean";
    public static final String PROTOBUF_SAMPLE_NAME = "Jack Proto";
    public static final int PROTOBUF_SAMPLE_AGE = 9;

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @Override
    public void configure(CamelContext context) {
        ProtobufDataFormat protobuf = new ProtobufDataFormat();
        protobuf.setLibrary(ProtobufLibrary.Jackson);
        protobuf.setUnmarshalType(JsonNode.class);
        protobuf.setAutoDiscoverSchemaResolver(Boolean.TRUE.toString());
        context.getRegistry().bind(PROTOBUF_BEAN, protobuf);

        try {
            ProtobufSchema schema = new ProtobufMapper().generateSchemaFor(MyData.class);
            SchemaResolver resolver = ex -> schema;
            context.getRegistry().bind("schema-resolver", SchemaResolver.class, resolver);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.setBody(ex -> new MyData(PROTOBUF_SAMPLE_NAME, PROTOBUF_SAMPLE_AGE))
                .log("Will marshal: ${body}")
                .marshal(PROTOBUF_BEAN)
                .log("Marshal: ${body}")
                .process(ex -> {
                    byte[] protobufData = ex.getIn().getBody(byte[].class);
                    assertNotNull(protobufData);
                    assertTrue(protobufData.length > 0);
                })
                .toF("mock:%s", getResultMockName())
                .log("Will unmarshal: ${body}")
                .unmarshal(PROTOBUF_BEAN)
                .log("Unmarshal: ${body}")
                .process(ex -> {
                    JsonNode data = ex.getIn().getBody(JsonNode.class);
                    assertEquals(PROTOBUF_SAMPLE_NAME, data.get("name").asText());
                    assertEquals(PROTOBUF_SAMPLE_AGE, data.get("age").asInt());
                })
                .toF("mock:%s", getResultMockName());
    }

    public static class MyData {

        private final String name;

        private final int age;

        public MyData(String name, int age) {
            this.name = name;
            this.age = age;
        }

        // getters are needed for marshall

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}