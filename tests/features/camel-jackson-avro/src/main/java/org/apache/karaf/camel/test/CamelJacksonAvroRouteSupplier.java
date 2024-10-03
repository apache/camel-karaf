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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.SchemaResolver;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.dataformat.AvroDataFormat;
import org.apache.camel.model.dataformat.AvroLibrary;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.avro.AvroMapper;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;

@Component(
        name = "karaf-camel-jackson-avro-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelJacksonAvroRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    public static final String AVRO_BEAN = "avroBean";
    public static final String AVRO_SAMPLE_NAME = "Jack Aveiro";
    public static final int AVRO_SAMPLE_AGE = 99;

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @Override
    public void configure(CamelContext context) {
        AvroDataFormat avro = new AvroDataFormat();
        avro.setLibrary(AvroLibrary.Jackson);
        avro.setUnmarshalType(JsonNode.class);
        avro.setAutoDiscoverSchemaResolver(Boolean.TRUE.toString());
        context.getRegistry().bind(AVRO_BEAN, avro);

        try {
            AvroSchema schema = new AvroMapper().schemaFor(MyData.class);
            SchemaResolver resolver = ex -> schema;
            context.getRegistry().bind("schema-resolver", SchemaResolver.class, resolver);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.setBody(ex -> new MyData(AVRO_SAMPLE_NAME, AVRO_SAMPLE_AGE))
            .log("Will marshal: ${body}")
            .marshal(AVRO_BEAN)
            .log("Marshal: ${body}")
            .process(ex -> {
                byte[] avroData = ex.getIn().getBody(byte[].class);
                assertNotNull(avroData);
                assertTrue(avroData.length > 0);
            })
            .toF("mock:%s", getResultMockName())
            .log("Will unmarshal: ${body}")
            .unmarshal(AVRO_BEAN)
            .log("Unmarshal: ${body}")
            .process(ex -> {
                JsonNode data = ex.getIn().getBody(JsonNode.class);
                assertEquals(AVRO_SAMPLE_NAME, data.get("name").asText());
                assertEquals(AVRO_SAMPLE_AGE, data.get("age").asInt());
            })
            .toF("mock:%s", getResultMockName());
    }

    public static class MyData {

        private String name;

        private int age;

        public MyData(String name, int age) {
            this.name = name;
            this.age = age;
        }

        // getters are needed for marshal

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}