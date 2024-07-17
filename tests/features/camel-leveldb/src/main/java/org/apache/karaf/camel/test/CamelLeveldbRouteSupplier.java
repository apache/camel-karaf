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
import static org.apache.camel.builder.Builder.header;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.leveldb.LevelDBAggregationRepository;
import org.apache.camel.component.leveldb.serializer.JacksonLevelDBSerializer;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "karaf-camel-leveldb-test",
        immediate = true,
        service = CamelLeveldbRouteSupplier.class
)
public class CamelLeveldbRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    static class StringAggregationStrategy implements AggregationStrategy {

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                return newExchange;
            }
            String body1 = oldExchange.getIn().getBody(String.class);
            String body2 = newExchange.getIn().getBody(String.class);

            oldExchange.getIn().setBody(body1 + body2);
            return oldExchange;
        }
    }

    @Override
    public boolean consumerEnabled() {
        return false;
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute
                .setHeader("id", constant("id1"))
                .setBody(constant("O"))
                .to("direct:aggregate")
                .log("sent O")
                .setBody(constant("K"))
                .to("direct:aggregate")
                .log("sent K");

        LevelDBAggregationRepository repo =
                new LevelDBAggregationRepository("repo1",
                        "%s/leveldb.dat".formatted(System.getProperty("project.target")));
        repo.setSerializer(new JacksonLevelDBSerializer());

        builder.from("direct:aggregate")
                .id("aggregate route")
                .aggregate(header("id"), new StringAggregationStrategy())
                .completionSize(2)
                .aggregationRepository(repo)
                .toF("mock:%s", getResultMockName())
                .end();
    }
}

