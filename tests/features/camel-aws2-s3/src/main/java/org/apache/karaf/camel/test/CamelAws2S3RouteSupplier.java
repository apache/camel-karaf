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
import org.apache.camel.component.aws2.s3.AWS2S3Component;
import org.apache.camel.component.aws2.s3.AWS2S3Configuration;
import org.apache.camel.component.aws2.s3.AWS2S3Constants;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(name = "karaf-camel-aws2-s3-test", immediate = true, service = CamelRouteSupplier.class)
public class CamelAws2S3RouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String HOST = System.getProperty("localstack.s3.host");
    private static final String PORT = System.getProperty("localstack.s3.port");
    private static final String REGION = System.getProperty("localstack.s3.region");
    private static final String ACCESS_KEY = System.getProperty("localstack.s3.accessKey");
    private static final String SECRET_KEY = System.getProperty("localstack.s3.secretKey");
    private static final String COMPONENT_NAME = "aws2-s3";
    private static final String BUCKET_NAME = "test-bucket";

    @Override
    public void configure(CamelContext camelContext) {
        final AWS2S3Component aws2S3Component = new AWS2S3Component();
        final AWS2S3Configuration configuration = new AWS2S3Configuration();
        configuration.setAccessKey(ACCESS_KEY);
        configuration.setSecretKey(SECRET_KEY);
        configuration.setRegion(REGION);
        configuration.setOverrideEndpoint(true);
        configuration.setUriEndpointOverride(String.format("http://%s:%s", HOST, PORT));
        aws2S3Component.setConfiguration(configuration);
        camelContext.addComponent(COMPONENT_NAME, aws2S3Component);
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder -> builder.fromF("%s://%s", COMPONENT_NAME, BUCKET_NAME).log("Received successfully: ${body}");
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.setHeader(AWS2S3Constants.KEY, builder.constant("hello.txt"))
                .setBody(builder.constant("Hello AWS2S3!"))
                .toF("%s://%s?autoCreateBucket=true", COMPONENT_NAME, BUCKET_NAME)
                .log("Sent successfully: ${body}");
    }
}