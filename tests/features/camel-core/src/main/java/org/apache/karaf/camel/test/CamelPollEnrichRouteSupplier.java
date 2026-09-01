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

/**
 * Covers the resolution of the internal {@code simple-no-file} language in OSGi.
 * <p>
 * When a {@code pollEnrich} is given a {@code simple} expression, {@code PollEnrichReifier} rewrites it to the
 * {@code simple-no-file} language, so starting this route makes the Camel context resolve that name. Apache Camel
 * ships no service file for it — {@code DefaultLanguageResolver} resolves it through a hardcoded branch — whereas
 * camel-karaf's {@code OsgiLanguageResolver} goes through the OSGi service registry. Without the service file in
 * the camel-core-languages bundle, this route fails to start with a {@code NoSuchLanguageException}.
 *
 * @see <a href="https://github.com/apache/camel-karaf/issues/707">issue #707</a>
 */
@Component(
        name = "karaf-camel-poll-enrich-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelPollEnrichRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String FILE_NAME_VARIABLE = "sourceFileName";
    private static final String SOURCE_FILE_NAME = "poll-enrich-source.txt";
    private static final long POLL_TIMEOUT_MS = 10000L;

    public String getBaseDir() {
        return "%s/poll-enrich".formatted(System.getProperty("project.target"));
    }

    @Override
    protected boolean consumerEnabled() {
        // Everything happens in the producer route: it seeds the file, then polls it back.
        return false;
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        String directory = getBaseDir();
        producerRoute
                .setVariable(FILE_NAME_VARIABLE, builder.constant(SOURCE_FILE_NAME))
                .setBody(builder.constant("OK"))
                .toF("file:%s?fileName=${variable.%s}", directory, FILE_NAME_VARIABLE)
                // Clear the body so that only a successful enrichment can restore it.
                .setBody(builder.constant(""))
                .pollEnrich()
                    .simple("file:%s?fileName=${variable.%s}&initialDelay=0&delay=100"
                            .formatted(directory, FILE_NAME_VARIABLE))
                .timeout(POLL_TIMEOUT_MS)
                .toF("mock:%s", getResultMockName());
    }
}
