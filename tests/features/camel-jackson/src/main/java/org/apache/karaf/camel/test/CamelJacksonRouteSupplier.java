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
import static org.junit.Assert.assertNull;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import com.fasterxml.jackson.annotation.JsonInclude;

@Component(
        name = "karaf-camel-jackson-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelJacksonRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    public static final String JSON_SAMPLE_NAME = "Jack";
    public static final int JSON_SAMPLE_AGE = 9;

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MyData {
        private String name;
        private String nikname;
        private int age;

        // empty constructor is needed by default
        public MyData() {};

        // getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNikname() {
            return nikname;
        }

        public void setNikname(String nikname) {
            this.nikname = nikname;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.log("Will unmarshal: ${body}")
                .unmarshal().json(JsonLibrary.Jackson, MyData.class)
                .log("Unmarshal: ${body}")
                .process(ex -> {
                    MyData data = ex.getIn().getBody(MyData.class);
                    assertEquals(JSON_SAMPLE_NAME, data.getName());
                    assertNull(data.getNikname());
                    assertEquals(JSON_SAMPLE_AGE, data.getAge());
                })
                .log("Will marshal: ${body}")
                .marshal().json(JsonLibrary.Jackson)
                .log("Marshal: ${body}")
                .toF("mock:%s", getResultMockName());
    }
}