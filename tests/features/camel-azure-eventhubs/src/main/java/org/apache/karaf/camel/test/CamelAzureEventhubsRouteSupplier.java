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

import static org.apache.camel.builder.Builder.method;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.eventhubs.EventHubsComponent;
import org.apache.camel.component.azure.eventhubs.EventHubsConfiguration;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;

@Component(
        name = "karaf-camel-azureeventhubs-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelAzureEventhubsRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String TEST_ACCOUNT = System.getProperty("azure.accountName");
    private static final String TEST_ACCOUNT_KEY = System.getProperty("azure.accountKey");
    public static final String TEST_CONTAINER = "mycontainer";
    private final AtomicBoolean isReceived = new AtomicBoolean();
    private String connectionString;

    @Override
    public void configure(CamelContext camelContext) {
        connectionString = System.getProperty("azure.connectionString");
        final StorageSharedKeyCredential credential =
                new StorageSharedKeyCredential(TEST_ACCOUNT, TEST_ACCOUNT_KEY);

        final String host = System.getProperty("azure.host");
        final String port = System.getProperty("azure.port");
        final String endpoint = "http://" + host + ":" + port + "/" + TEST_ACCOUNT;

        BlobContainerAsyncClient blobClient = new BlobContainerClientBuilder()
                .endpoint(endpoint)
                .containerName(TEST_CONTAINER)
                .credential(credential)
                .buildAsyncClient();
        blobClient.createIfNotExists().block();

        EventHubsConfiguration config = new EventHubsConfiguration();
        config.setCheckpointStore(new BlobCheckpointStore(blobClient));
        config.setBlobAccountName(TEST_ACCOUNT);
        config.setBlobContainerName(TEST_CONTAINER);
        config.setBlobStorageSharedKeyCredential(credential);
        config.setBlobAccessKey(TEST_ACCOUNT_KEY);
        config.setConnectionString(connectionString);

        EventHubsComponent component = new EventHubsComponent();
        component.setConfiguration(config);
        camelContext.addComponent("azure-eventhubs", component);
    }


    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder-> builder.fromF("azure-eventhubs:?connectionString=%sEntityPath=eh1", connectionString)
                .log("Received message from Event Hub: ${body}")
                .process(ex -> isReceived.set(true));
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.log("sending a message: ${body}")
                //loop to send until the consumer is started
                .loopDoWhile(method(this, "shouldContinueSending"))
                    .delay(500)
                    .toF("azure-eventhubs:?connectionString=%sEntityPath=eh1", connectionString)
                    .log("message sent")
                .end();
    }

    public boolean shouldContinueSending() {
        return !isReceived.get();
    }
}

