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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Function;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelComponentResultMockBased;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "karaf-camel-jetty-test",
        immediate = true
)
public class CamelJettyComponent extends AbstractCamelComponentResultMockBased {

    private final int port = getNextAvailablePort();

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder -> builder.from("jetty://http://localhost:%s/jettyTest".formatted(port)).transform(builder.constant("OK"));
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.log("calling http endpoint")
                .process(new HttpClientProcessor());
    }

    class HttpClientProcessor implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {

            HttpClient client = HttpClient.newHttpClient();

            // Create a URI for the request
            URI uri = URI.create("http://localhost:%s/jettyTest".formatted(port));

            // Create a HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}

