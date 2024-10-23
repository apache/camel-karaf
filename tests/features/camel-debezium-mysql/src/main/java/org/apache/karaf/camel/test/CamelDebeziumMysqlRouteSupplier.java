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

import com.mysql.cj.jdbc.MysqlDataSource;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.RouteDefinition;
import org.apache.kafka.connect.data.Struct;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import static org.apache.camel.builder.Builder.constant;
import static org.apache.camel.builder.Builder.simple;

@Component(
        name = "karaf-camel-debezium-mysql-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelDebeziumMysqlRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    public void configure(CamelContext context) {
        MysqlDataSource db = new MysqlDataSource();
        db.setServerName(System.getProperty("mysql.host"));
        db.setPort(Integer.getInteger("mysql.port"));
        db.setDatabaseName(System.getProperty("mysql.database"));
        db.setUser(System.getProperty("mysql.username"));
        db.setPassword(System.getProperty("mysql.password"));

        context.getComponent("sql", SqlComponent.class).setDataSource(db);
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                (RouteDefinition) builder.fromF("debezium-mysql:debezium-mysql-example-01?databaseHostname=%s&databasePort=%s"
                                + "&databaseUser=%s&databasePassword=%s"
                                + "&topicPrefix=embedded-debezium&offsetStorageFileName=offset-01.data&offsetStorage=org.apache.kafka.connect.storage.FileOffsetBackingStore"
                                + "&databaseIncludeList=%s&tableIncludeList=%s&databaseServerId=184054&schemaHistoryInternal=io.debezium.storage.file.history.FileSchemaHistory&schemaHistoryInternalFileFilename=schema-history-01.data",
                                System.getProperty("mysql.host"), System.getProperty("mysql.port"),
                                System.getProperty("mysql.username"), System.getProperty("mysql.password"),
                                System.getProperty("mysql.database"), System.getProperty("mysql.table"))
                        .log("received message ${body}")
                        .choice()
                            .when(simple("${body.toString} contains 'id'"))
                                .process(exchange -> exchange.getIn().setBody(exchange.getIn().getBody(Struct.class).get("id")))
                            .otherwise()
                                .stop()
                        .endChoice()
                        .end();
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute
                .log("insert new product")
                .setBody(constant(new Object[] { 1, "scooter", "Small 2-wheel yellow scooter", 5.54 }))
                .toF("sql:insert into %s (id, name, description, weight) values (#, #, #, #)", System.getProperty("mysql.table"));
    }
}

