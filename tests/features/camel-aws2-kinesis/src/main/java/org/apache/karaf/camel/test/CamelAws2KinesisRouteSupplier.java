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

import org.apache.camel.component.aws2.kinesis.Kinesis2Component;
import org.apache.camel.component.aws2.kinesis.Kinesis2Configuration;
import org.apache.camel.component.aws2.kinesis.Kinesis2Constants;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import static org.apache.camel.builder.Builder.constant;

@Component(name = "karaf-camel-aws2-kinesis-test", immediate = true, service = CamelRouteSupplier.class)
public class CamelAws2KinesisRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String HOST = System.getProperty("localstack.kinesis.host");
    private static final String PORT = System.getProperty("localstack.kinesis.port");
    private static final String REGION = System.getProperty("localstack.kinesis.region");
    private static final String ACCESS_KEY = System.getProperty("localstack.kinesis.accessKey");
    private static final String SECRET_KEY = System.getProperty("localstack.kinesis.secretKey");
    private static final String STREAM_NAME = System.getProperty("localstack.kinesis.streamName");
    private static final String COMPONENT_NAME = "aws2-kinesis";

    @Override
    public void configure(CamelContext camelContext) {
        final Kinesis2Component kinesis2Component = new Kinesis2Component();
        final Kinesis2Configuration configuration = new Kinesis2Configuration();
        configuration.setAccessKey(ACCESS_KEY);
        configuration.setSecretKey(SECRET_KEY);
        configuration.setRegion(REGION);
        configuration.setOverrideEndpoint(true);
        configuration.setUriEndpointOverride(String.format("http://%s:%s", HOST, PORT));
        kinesis2Component.setConfiguration(configuration);
        camelContext.addComponent(COMPONENT_NAME, kinesis2Component);
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        configureConsumer(producerRoute
                .setBody(constant("Hello Kinesis"))
                .setHeader(Kinesis2Constants.PARTITION_KEY, constant("partition-1"))
                .toF("%s://%s", COMPONENT_NAME, STREAM_NAME));
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                builder.fromF("%s://%s", COMPONENT_NAME, STREAM_NAME)
                        .log("Received successfully: ${body}")
                        .setBody(constant("OK"));
    }
}