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

import java.util.Map;
import java.util.function.Function;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;


@Component(
        name = "karaf-camel-fastjson-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelFastjsonRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                builder.from("direct:consumejson")
                        .log("received message ${body}")
                        .unmarshal().json(JsonLibrary.Fastjson,Map.class)
                        .log("unmarshalled ${body}")
                        .process(ex -> {
                            Map<?, ?> data = ex.getIn().getBody(Map.class);
                            ex.getIn().setBody(data.get("key"));
                        });
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.process(ex -> ex.getIn().setBody(Map.of("key", "OK")))
                .marshal().json(JsonLibrary.Fastjson)
                .log("serialized json ${body}")
                .to("direct:consumejson");
    }
}

