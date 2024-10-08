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
import org.apache.camel.component.consul.ConsulComponent;
import org.apache.camel.component.consul.ConsulConstants;
import org.apache.camel.component.consul.endpoint.ConsulHealthActions;
import org.apache.camel.component.consul.endpoint.ConsulKeyValueActions;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "karaf-camel-consul-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelConsulRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String KEY= "consulKey";

    @Override
    public void configure(CamelContext context) {
        ConsulComponent component = new ConsulComponent(context);
        component.getConfiguration().setUrl("http://localhost:%s".formatted(System.getProperty("consul.port")));
        context.addComponent("consul", component);
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                builder.fromF("consul:kv?key=%s&valueAsString=true", KEY)
                        .log("received message ${body}");
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute
                .setHeader(ConsulConstants.CONSUL_ACTION, constant(ConsulHealthActions.SERVICE_INSTANCES))
                .setHeader(ConsulConstants.CONSUL_SERVICE, constant("service"))
                .to("consul:health")
                .log("Consul health check: ${body}")
                .setHeader(ConsulConstants.CONSUL_ACTION, constant(ConsulKeyValueActions.PUT))
                .setHeader(ConsulConstants.CONSUL_KEY, constant(KEY))
                .setBody(constant("OK"))
                .log("sending message")
                .to("consul:kv")
                .log("message sent to consul : ${body}");
    }
}

