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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;


@Component(
        name = "karaf-camel-mail-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelMailRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {


    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                builder.fromF(
                        "pop3://camel@localhost:%s?password=foo&initialDelay=100&delay=500",System.getProperty("pop3.port"))
                        .log("received message ${body} from:${header.from} to:${header.to} subj: ${header.subject}");
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.log("calling mail endpoint")
                .setBody(builder.constant("OK"))
                .setHeader("Subject", builder.constant("Test"))
                .setHeader("From", builder.constant("origin@localhost"))
                .setHeader("To", builder.constant("camel@localhost"))
                .toF("smtp://camel@localhost:%s?password=foo",System.getProperty("smtp.port"));
    }
}

