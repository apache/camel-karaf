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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(name = "karaf-camel-base64-test", immediate = true, service = CamelRouteSupplier.class)
public class CamelBase64RouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String ORIGINAL_STRING = "Hello Base64! 1234567890 ~`!@#$%^&*()-_=+[]{}|;:'\",.<>?/";
    private static final int ENCODER_LINE_LENGTH = 9;

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.process(ex -> ex.getIn().setBody(ORIGINAL_STRING))
            .log("Will encode: ${body}")
            .marshal().base64(ENCODER_LINE_LENGTH, System.lineSeparator(), true)
            .log("Will decode: ${body}")
            .unmarshal().base64()
            .log("Decoded: ${body}")
            .toF("mock:%s", getResultMockName());
    }

}