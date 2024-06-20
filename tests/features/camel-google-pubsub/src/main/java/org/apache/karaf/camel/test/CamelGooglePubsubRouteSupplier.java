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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.google.pubsub.GooglePubsubComponent;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(name = "karaf-camel-google-pubsub-test", immediate = true, service = CamelRouteSupplier.class)
public class CamelGooglePubsubRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String TEST_PROJECT = System.getProperty("pubsub.project");
    private static final String TEST_HOST = System.getProperty("pubsub.host") + ":" + System.getProperty("pubsub.port");
    private static final String TEST_TOPIC = System.getProperty("pubsub.topic");
    private static final String TEST_SUBSCRIPTION = System.getProperty("pubsub.subscription");
    private static final String GOOGLE_PUB_SUB_MESSAGE = "Hello, Google Pub/Sub!";
    private static final String GOOGLE_PUBSUB_COMPONENT_ID = "google-pubsub";

    @Override
    public void configure(CamelContext camelContext) {
        final GooglePubsubComponent googlePubsubComponent = new GooglePubsubComponent();
        googlePubsubComponent.setAuthenticate(false);
        googlePubsubComponent.setEndpoint(TEST_HOST);

        camelContext.addComponent(GOOGLE_PUBSUB_COMPONENT_ID, googlePubsubComponent);
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.setBody(builder.constant(GOOGLE_PUB_SUB_MESSAGE))
                .toF("%s:%s:%s", GOOGLE_PUBSUB_COMPONENT_ID, TEST_PROJECT, TEST_TOPIC)
                .log("Sent message to PubSub: ${body}");
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder -> builder.fromF("%s:%s:%s", GOOGLE_PUBSUB_COMPONENT_ID, TEST_PROJECT, TEST_SUBSCRIPTION)
                .log("Received message from PubSub: ${body}");
    }
}