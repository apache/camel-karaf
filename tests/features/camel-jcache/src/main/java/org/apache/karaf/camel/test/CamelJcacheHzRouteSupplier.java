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
import org.apache.camel.component.jcache.JCacheConstants;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import com.hazelcast.cache.HazelcastCachingProvider;

@Component(
        name = "karaf-camel-jcache-hz-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelJcacheHzRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    public static final String TEST_CACHE_NAME = "mycache";
    public static final String PROVIDER_CLASS = "com.hazelcast.cache.HazelcastCachingProvider";

    @Override
    public void configure(CamelContext camelContext) {
        System.setProperty("hazelcast.jcache.provider.type", "member");
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                builder.fromF("jcache://%s?cachingProvider=%s&lookupProviders=true", TEST_CACHE_NAME, PROVIDER_CLASS)
                        .log("received message ${body}");
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.log("Storing in cache")
                .setBody(builder.constant("OK"))
                .setHeader(JCacheConstants.ACTION).constant("PUT")
                .setHeader(JCacheConstants.KEY).constant("OK")
                .toF("jcache://%s?cachingProvider=%s&lookupProviders=true", TEST_CACHE_NAME, PROVIDER_CLASS)
                .log("Cached value: ${body}");
    }
}

