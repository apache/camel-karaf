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
import org.apache.camel.component.ehcache.EhcacheConstants;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.expiry.ExpiryPolicy;
import org.osgi.service.component.annotations.Component;


@Component(
        name = "karaf-camel-ehcache-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelEhcacheRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    public static final String TEST_CACHE_NAME = "mycache";

    @Override
    public void configure(CamelContext camelContext) {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache(TEST_CACHE_NAME, CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                Object.class, Object.class, ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(100, EntryUnit.ENTRIES))
                        .withExpiry(ExpiryPolicy.NO_EXPIRY)
                )
                .build(true);

        camelContext.getRegistry().bind("cacheManager",cacheManager);
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                builder.fromF("ehcache://%s?cacheManager=#cacheManager", TEST_CACHE_NAME)
                        .log("received message ${body}");
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.log("Storing in cache")
                .setBody(builder.constant("OK"))
                .setHeader(EhcacheConstants.ACTION).constant(EhcacheConstants.ACTION_PUT)
                .setHeader(EhcacheConstants.KEY).constant("OK")
                .toF("ehcache://%s?cacheManager=#cacheManager", TEST_CACHE_NAME)
                .log("Cached value: ${body}");
    }
}

