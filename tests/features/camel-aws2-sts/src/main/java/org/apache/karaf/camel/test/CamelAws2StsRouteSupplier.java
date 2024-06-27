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
import org.apache.camel.component.aws2.sts.STS2Component;
import org.apache.camel.component.aws2.sts.STS2Configuration;
import org.apache.camel.component.aws2.sts.STS2Constants;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(name = "karaf-camel-aws2-sts-test", immediate = true, service = CamelRouteSupplier.class)
public class CamelAws2StsRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String HOST = System.getProperty("localstack.sts.host");
    private static final String PORT = System.getProperty("localstack.sts.port");
    private static final String REGION = System.getProperty("localstack.sts.region");
    private static final String ACCESS_KEY = System.getProperty("localstack.sts.accessKey");
    private static final String SECRET_KEY = System.getProperty("localstack.sts.secretKey");
    private static final String COMPONENT_NAME = "aws2-sts";

    @Override
    public void configure(CamelContext camelContext) {
        final STS2Component aws2StsComponent = new STS2Component();
        final STS2Configuration configuration = new STS2Configuration();
        configuration.setAccessKey(ACCESS_KEY);
        configuration.setSecretKey(SECRET_KEY);
        configuration.setRegion(REGION);
        configuration.setOverrideEndpoint(true);
        configuration.setUriEndpointOverride(String.format("http://%s:%s", HOST, PORT));
        aws2StsComponent.setConfiguration(configuration);
        camelContext.addComponent(COMPONENT_NAME, aws2StsComponent);
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        configureConsumer(producerRoute.setHeader(STS2Constants.ROLE_ARN, builder.constant("arn:123"))
                .setHeader(STS2Constants.ROLE_SESSION_NAME, builder.constant("groot"))
                .to("aws2-sts://test?operation=assumeRole")
                .log("Role assumed successfully: ${body}")
                .setBody(builder.constant("OK")));
    }

    @Override
    protected boolean consumerEnabled() {
        return false;
    }
}