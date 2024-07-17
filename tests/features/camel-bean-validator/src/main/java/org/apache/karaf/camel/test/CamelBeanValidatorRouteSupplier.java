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
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.osgi.service.component.annotations.Component;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Component(
        name = "karaf-camel-bean-validator-test",
        immediate = true,
        service = CamelBeanValidatorRouteSupplier.class
)
public class CamelBeanValidatorRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    public record User(@NotNull String name, @Min(18) int age) {}

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.setBody(constant(new User("John", 90)))
                .to("bean-validator://x")
                .log("body {body}")
                .toF("mock:%s", getResultMockName());
    }
}

