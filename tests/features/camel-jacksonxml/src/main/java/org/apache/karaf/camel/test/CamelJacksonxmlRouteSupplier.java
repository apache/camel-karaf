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
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import com.fasterxml.jackson.annotation.JsonInclude;

@Component(
        name = "karaf-camel-jacksonxml-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelJacksonxmlRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    public static final String XML_SAMPLE_NAME = "Jack";
    public static final int XML_SAMPLE_AGE = 9;

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MyData {
        private String name;
        private String nickname;
        private int age;

        // empty constructor is needed by default
        public MyData() {
        };

        // getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
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
        .unmarshal().jacksonXml(MyData.class)
        .log("Unmarshal: ${body}")
        .process(ex -> {
            MyData data = ex.getIn().getBody(MyData.class);
            assertEquals(XML_SAMPLE_NAME, data.getName());
            assertNull(data.getNickname());
            assertEquals(XML_SAMPLE_AGE, data.getAge());
        })
        .log("Will marshal: ${body}")
        .marshal().jacksonXml()
        .log("Marshal: ${body}")
        .toF("mock:%s", getResultMockName());
    }
}