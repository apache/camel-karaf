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
        name = "karaf-camel-ftp-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelFtpRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        //uploads a file "test.txt" containing "OK"
        producerRoute.log("storing file in ftp")
                .setBody(builder.constant("OK"))
                .toF("ftp://localhost:%s/test.txt?username=scott&password=tiger&binary=true&passiveMode=true&delay=1000",
                        System.getProperty("ftp.port"));
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        //download the file "test.txt" and get the content
        return builder -> builder.fromF("ftp://localhost:%s/test.txt?username=scott&password=tiger&binary=true&passiveMode=true&delay=1000",
                        System.getProperty("ftp.port"))
                .convertBodyTo(String.class);
    }
}

