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

import java.util.function.Function;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;


@Component(
        name = "karaf-camel-gson-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelGsonRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    public static class MyData {
        private String name;
        private int age;

        public MyData(String name, int age) {
            this.name = name;
            this.age = age;
        }

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                builder.from("direct:consumejson")
                        .log("received message ${body}")
                        .unmarshal().json(JsonLibrary.Gson,MyData.class)
                        .process( ex -> {
                            MyData data = ex.getIn().getBody(MyData.class);
                            ex.getIn().setBody(data.getName());
                        });
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.process( ex -> ex.getIn().setBody(new MyData("OK",3)))
                .marshal().json(JsonLibrary.Gson)
                .log("serialized json ${body}")
                .to("direct:consumejson");
    }
}

