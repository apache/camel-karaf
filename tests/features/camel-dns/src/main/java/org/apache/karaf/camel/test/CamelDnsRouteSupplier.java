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
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;
import org.xbill.DNS.NioClient;

@Component(
        name = "karaf-camel-dns-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelDnsRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    public static final String LOCALHOST = "localhost";

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @Override
    public void cleanUp(CamelContext camelContext) {
        // Close it explicitly to prevent NoClassDefFoundError after the test
        NioClient.close();
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.log("Will get IP")
        .setHeader("dns.domain", builder.constant(LOCALHOST))
        .to("dns:ip")
        .log("IP: ${body}")
        .toF("mock:%s", getResultMockName())
        .log("Will lookup")
        .setHeader("dns.name", builder.constant(LOCALHOST))
        .to("dns:lookup")
        .log("Lookup: ${body}")
        .toF("mock:%s", getResultMockName())
        .log("Will dig")
        .setHeader("dns.type", builder.constant("A"))
        .to("dns:dig")
        .log("Dig: ${body}")
        .toF("mock:%s", getResultMockName());
    }
}