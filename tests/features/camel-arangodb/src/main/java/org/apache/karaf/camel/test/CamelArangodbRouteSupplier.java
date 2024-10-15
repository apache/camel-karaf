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
import static org.apache.camel.component.arangodb.ArangoDbConstants.AQL_QUERY;
import static org.apache.camel.component.arangodb.ArangoDbConstants.ARANGO_KEY;
import static org.apache.camel.component.arangodb.ArangoDbConstants.RESULT_CLASS_TYPE;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.arangodb.ArangoDbComponent;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;

@Component(name = "karaf-camel-arangodb-test", immediate = true, service = CamelRouteSupplier.class)
public class CamelArangodbRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String DATABASE_NAME = "testdb";
    private static final String COLLECTION_NAME = "testcol";

    @Override
    public void configure(CamelContext camelContext) {
        final int arangoPort = Integer.getInteger("arango.port");
        ArangoDB arangoDb = new ArangoDB.Builder().host("localhost", arangoPort).build();

        arangoDb.createDatabase(DATABASE_NAME);
        ArangoDatabase arangoDatabase = arangoDb.db(DATABASE_NAME);
        arangoDatabase.createCollection(COLLECTION_NAME);

        ArangoDbComponent arangoDbComponent = new ArangoDbComponent();
        arangoDbComponent.setArangoDB(arangoDb);
        arangoDbComponent.getConfiguration().setHost("localhost");
        arangoDbComponent.getConfiguration().setPort(arangoPort);

        camelContext.addComponent("arangodb", arangoDbComponent);
    }

    @Override
    public void cleanUp(CamelContext camelContext) {
        if (camelContext.hasComponent("arangodb") instanceof ArangoDbComponent arangoDbComponent) {
            arangoDbComponent.getArangoDB().shutdown();
        }
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {

        BaseDocument myObject0 = new BaseDocument();
        myObject0.setKey("key0");
        myObject0.addAttribute("c", 42);


        BaseDocument myObject = new BaseDocument();
        myObject.setKey("key1");
        myObject.addAttribute("a", "OK for later");
        myObject.addAttribute("b", 42);

        BaseDocument myUpdatedObject = new BaseDocument();
        myUpdatedObject.setKey("key1");
        myUpdatedObject.addAttribute("a", "OK for later");
        myUpdatedObject.updateAttribute("a","OK");


        //to add the mock endpoint at the end of the route, call configureConsumer
        configureConsumer(
            producerRoute
                    //insert
                    .log("insert 2 documents")
                    .setBody(builder.constant(myObject0))
                    .toF("arangodb:%s?documentCollection=%s&operation=SAVE_DOCUMENT", DATABASE_NAME, COLLECTION_NAME)
                    .setBody(builder.constant(myObject))
                    .toF("arangodb:%s?documentCollection=%s&operation=SAVE_DOCUMENT", DATABASE_NAME, COLLECTION_NAME)
                    //update
                    .log("update document key")
                    .setBody(constant(myUpdatedObject))
                    .setHeader(ARANGO_KEY,constant("key1"))
                    .toF("arangodb:%s?documentCollection=%s&operation=UPDATE_DOCUMENT", DATABASE_NAME, COLLECTION_NAME)
                    //delete
                    .log("delete document key0")
                    .setBody(constant("key0"))
                    .toF("arangodb:%s?documentCollection=%s&operation=DELETE_DOCUMENT", DATABASE_NAME, COLLECTION_NAME)
                    //query
                    .setHeader(AQL_QUERY,constant("FOR doc IN %s RETURN doc".formatted(COLLECTION_NAME)))
                    .setHeader(RESULT_CLASS_TYPE, constant(BaseDocument.class))
                    .toF("arangodb:%s?operation=AQL_QUERY", DATABASE_NAME, COLLECTION_NAME)
                    .log("after query ${body}")
                    .process( exchange ->
                            exchange.getIn().setBody(
                                            ((BaseDocument) exchange.getIn().getMandatoryBody(List.class).get(0)).getAttribute("a")))
                    .log("end: ${body}")
        );

    }

    @Override
    protected boolean consumerEnabled() {
        return false;
    }
}