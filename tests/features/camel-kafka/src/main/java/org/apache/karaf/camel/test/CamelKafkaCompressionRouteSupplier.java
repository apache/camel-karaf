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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import java.util.function.Function;


@Component(
        name = "karaf-camel-kafka-compression-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelKafkaCompressionRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    protected String getResultMockName() {
        return "camel-kafka-test";
    }

    @Override
    public String getTestComponentName() {
        return "camel-kafka-test";
    }

    @Override
    public void configure(CamelContext camelContext) {
        KafkaComponent kafka = new KafkaComponent();
        KafkaConfiguration config = new KafkaConfiguration();
        config.setBreakOnFirstError(true);
        config.setBrokers(System.getProperty("kafka.server"));
        kafka.setConfiguration(config);
        camelContext.addComponent("kafka", kafka);
    }

    private static final String KAFKA_TOPIC = "testTopic";

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                builder.fromF("kafka://%s?autoOffsetReset=earliest", KAFKA_TOPIC)
                        .log("received kafka message ${body}");
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.log("calling kafka topic")
                .setBody(builder.constant("OK"))
                .toF("kafka://%s?compressionCodec=snappy", KAFKA_TOPIC)
                .log("kafka topic called");
    }
}

