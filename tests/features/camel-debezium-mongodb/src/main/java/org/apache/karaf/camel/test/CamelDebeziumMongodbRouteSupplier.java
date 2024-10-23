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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mongodb.MongoDbComponent;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.bson.Document;
import org.osgi.service.component.annotations.Component;

import static org.apache.camel.builder.Builder.constant;
import static org.apache.camel.builder.Builder.simple;
import static org.apache.camel.component.mongodb.MongoDbConstants.MONGO_ID;

@Component(
        name = "karaf-camel-debezium-mongodb-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelDebeziumMongodbRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    public void configure(CamelContext context) {
        MongoClient db = MongoClients.create(
                MongoClientSettings.builder()
                        .credential(MongoCredential.createCredential(System.getProperty("mongo.username"),
                                System.getProperty("mongo.authentication.database"), System.getProperty("mongo.password").toCharArray()))
                        .applyConnectionString(new ConnectionString(System.getProperty("mongo.connection")))
                        .build());
        context.getRegistry().bind("db", db);
        context.getComponent("mongodb", MongoDbComponent.class).setMongoConnection(null);
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                (RouteDefinition) builder.fromF("debezium-mongodb:debezium-mongodb-example-01?mongodbConnectionString=%s"
                                + "&mongodbAuthsource=%s&mongodbUser=%s&mongodbPassword=%s"
                                + "&topicPrefix=embedded-debezium&offsetStorageFileName=offset-01.data&offsetStorage=org.apache.kafka.connect.storage.FileOffsetBackingStore"
                                + "&databaseIncludeList=%s&schemaHistoryInternalFileFilename=schema-history-01.data",
                                System.getProperty("mongo.connection"),
                                System.getProperty("mongo.authentication.database"), System.getProperty("mongo.username"), System.getProperty("mongo.password"),
                                System.getProperty("mongo.database"))
                        .log("received message ${body}")
                        .choice()
                            .when(simple("${body.toString} contains 'weight'"))
                                .process(exchange -> exchange.getIn().setBody(exchange.getIn().getBody(Document.class).get("_id")))
                            .otherwise()
                                .stop()
                        .endChoice()
                        .end();
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute
                .log("insert new product")
                .setBody(constant(new Document(MONGO_ID, 1L)
                        .append("name", "scooter")
                        .append("description", "Small 2-wheel yellow scooter")
                        .append("quantity", 5)
                        .append("weight", 5.54).toJson()))
                .toF("mongodb:db?database=%s&collection=%s&operation=insert", System.getProperty("mongo.database"), System.getProperty("mongo.collection"));
    }
}

