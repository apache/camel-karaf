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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.RouteDefinition;
import org.apache.kafka.connect.data.Struct;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;
import org.postgresql.ds.PGSimpleDataSource;

@Component(
        name = "karaf-camel-debezium-postgres-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelDebeziumPostgresRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    public void configure(CamelContext context) {
        PGSimpleDataSource db = new PGSimpleDataSource();
        db.setServerNames(new String[]{System.getProperty("pgsql.host")});
        db.setPortNumbers(new int[]{Integer.getInteger("pgsql.port")});
        db.setUser(System.getProperty("pgsql.username"));
        db.setPassword(System.getProperty("pgsql.password"));
        db.setDatabaseName(System.getProperty("pgsql.database"));

        context.getComponent("sql", SqlComponent.class).setDataSource(db);
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                builder.fromF("debezium-postgres:debezium-postgres-example-01?databaseHostname=%s&databasePort=%s"
                                + "&databaseUser=%s&databasePassword=%s&databaseDbname=%s"
                                + "&topicPrefix=embedded-debezium&offsetStorageFileName=offset-01.data&pluginName=pgoutput"
                                + "&schemaIncludeList=%s&tableIncludeList=%s",
                                System.getProperty("pgsql.host"), System.getProperty("pgsql.port"),
                                System.getProperty("pgsql.username"), System.getProperty("pgsql.password"),
                                System.getProperty("pgsql.database"), System.getProperty("pgsql.schema"), System.getProperty("pgsql.table"))
                        .log("received message ${body}")
                        .process(exchange -> exchange.getIn().setBody(exchange.getIn().getBody(Struct.class).get("id")));
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute
                .log("insert new product")
                .setBody(constant(new Object[] { 1, "scooter", "Small 2-wheel yellow scooter", 5.54 }))
                .toF("sql:insert into %s (id, name, description, weight) values (#, #, #, #)", System.getProperty("pgsql.table"));
    }
}

