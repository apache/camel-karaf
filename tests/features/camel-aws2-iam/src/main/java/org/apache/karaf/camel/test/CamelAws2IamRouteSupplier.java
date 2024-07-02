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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.iam.IAM2Component;
import org.apache.camel.component.aws2.iam.IAM2Configuration;
import org.apache.camel.component.aws2.iam.IAM2Constants;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(name = "karaf-camel-aws2-iam-test", immediate = true, service = CamelRouteSupplier.class)
public class CamelAws2IamRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String REGION = System.getProperty("localstack.iam.region");
    private static final String ACCESS_KEY = System.getProperty("localstack.iam.accessKey");
    private static final String SECRET_KEY = System.getProperty("localstack.iam.secretKey");
    private static final String COMPONENT_NAME = "aws2-iam";

    @Override
    public void configure(CamelContext camelContext) {
        final IAM2Component aws2IamComponent = new IAM2Component();
        final IAM2Configuration configuration = new IAM2Configuration();
        configuration.setAccessKey(ACCESS_KEY);
        configuration.setSecretKey(SECRET_KEY);
        configuration.setRegion(REGION);
        aws2IamComponent.setConfiguration(configuration);
        camelContext.addComponent(COMPONENT_NAME, aws2IamComponent);
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        configureConsumer(producerRoute
                .setHeader(IAM2Constants.USERNAME, constant("testUser1"))
                .to("aws2-iam://test?operation=createUser")
                .setHeader(IAM2Constants.USERNAME, constant("testUser2"))
                .to("aws2-iam://test?operation=createUser")
                .log("User Created: ${body}")
                .to("aws2-iam://test?operation=deleteUser")
                .to("aws2-iam://test?operation=listUsers")
                .log("List users: ${body}")
                );
    }

    @Override
    protected boolean consumerEnabled() {
        return false;
    }
}