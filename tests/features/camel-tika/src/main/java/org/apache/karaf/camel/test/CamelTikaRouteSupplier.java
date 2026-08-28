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

@Component(
        name = "karaf-camel-tika-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelTikaRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        // Route the body through tika:parse. The value of this route is that it forces the
        // camel-tika feature (including tika-parser-text-module and its juniversalchardet
        // dependency, which were missing before issue #713) to resolve and run; a message
        // reaching the mock proves the feature is now wired correctly.
        producerRoute.log("Will parse: ${body}")
                    .to("tika:parse?tikaParseOutputFormat=text")
                    .convertBodyTo(String.class)
                    .log("Parsed: ${body}")
                    .toF("mock:%s", getResultMockName());
    }
}
