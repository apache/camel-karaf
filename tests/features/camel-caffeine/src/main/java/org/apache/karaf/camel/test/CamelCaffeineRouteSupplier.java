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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.caffeine.CaffeineConstants;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "karaf-camel-caffeine-test",
        immediate = true,
        service = CamelCaffeineRouteSupplier.class
)
public class CamelCaffeineRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.setBody(constant("OK"))
                .setHeader(CaffeineConstants.ACTION, constant(CaffeineConstants.ACTION_PUT))
                .setHeader(CaffeineConstants.KEY, constant("cachekey"))
                .to("caffeine-cache://testcache?statsEnabled=true")
                .setHeader(CaffeineConstants.ACTION, constant(CaffeineConstants.ACTION_GET))
                .setHeader(CaffeineConstants.KEY, constant("cachekey"))
                .to("caffeine-cache://testcache?statsEnabled=true")
                .log("received: ${body}")
                .toF("mock:%s", getResultMockName());
    }
}

