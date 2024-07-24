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

import static org.apache.camel.builder.Builder.constant;

import java.util.function.Function;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.cometd.server.http.JSONHttpTransport;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "karaf-camel-cometd-test",
        immediate = true,
        service = CamelCometdRouteSupplier.class
)
public class CamelCometdRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    public void configure(CamelContext camelContext) {
        JSONHttpTransport.class.getClass();
        camelContext.setApplicationContextClassLoader(this.getClass().getClassLoader());
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                builder.fromF("cometd://127.0.0.1:%s/service/test?baseResource=file:%s/test-classes/webapp&timeout=240000&"
                                + "interval=0&maxInterval=30000&multiFrameInterval=1500&jsonCommented=true&logLevel=2",
                                System.getProperty("cometd.port"), System.getProperty("project.target"))
                        .log("received message ${body}")
                        .setBody(constant("OK"));
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.toF("cometd://127.0.0.1:%s/service/test?baseResource=file:%s/test-classes/webapp&timeout=240000&"
                        + "interval=0&maxInterval=30000&multiFrameInterval=1500&jsonCommented=true&logLevel=2",
                System.getProperty("cometd.port"), System.getProperty("project.target"));
    }
}

