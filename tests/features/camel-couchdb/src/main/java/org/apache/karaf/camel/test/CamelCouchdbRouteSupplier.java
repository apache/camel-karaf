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

import java.util.function.Function;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.couchdb.CouchDbConstants;
import org.apache.camel.component.couchdb.CouchDbOperations;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "karaf-camel-couchdb-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelCouchdbRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String COUCHDB_BASE_URI = "couchdb:http://%s:%s/%s?username=%s&password=%s".formatted(
            System.getProperty("couchdb.host"),
            System.getProperty("couchdb.port"),
            "testdb",
            System.getProperty("couchdb.user"),
            System.getProperty("couchdb.pass"));

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder -> builder.from(COUCHDB_BASE_URI + "&deletes=true&updates=false")
                                .log("Received: ${body}");
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.log("Will save as document: ${body}")
                    .to(COUCHDB_BASE_URI + "&createDatabase=true")
                    .log("Document saved with id: ${header.%s}".formatted(CouchDbConstants.HEADER_DOC_ID))
                    .log("Will get document: ${header.%s}".formatted(CouchDbConstants.HEADER_DOC_ID))
                    .setHeader(CouchDbConstants.HEADER_METHOD, constant(CouchDbOperations.GET.toString()))
                    .to(COUCHDB_BASE_URI)
                    .log("Get document: ${body}")
                    .log("Will delete document")
                    .setHeader(CouchDbConstants.HEADER_METHOD, constant(CouchDbOperations.DELETE.toString()))
                    .to(COUCHDB_BASE_URI);
    }
}