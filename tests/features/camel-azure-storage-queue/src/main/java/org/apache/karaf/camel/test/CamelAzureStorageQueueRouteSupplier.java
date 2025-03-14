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
import org.apache.camel.component.azure.storage.queue.QueueComponent;
import org.apache.camel.component.azure.storage.queue.QueueConfiguration;
import org.apache.camel.component.azure.storage.queue.QueueConstants;
import org.apache.camel.component.azure.storage.queue.QueueOperationDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;

import static org.apache.camel.component.azure.storage.queue.CredentialType.SHARED_KEY_CREDENTIAL;
import static org.apache.camel.component.azure.storage.queue.QueueConstants.QUEUE_OPERATION;

@Component(name = "karaf-camel-azure-storage-queue-test", immediate = true, service = CamelRouteSupplier.class)
public class CamelAzureStorageQueueRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String TEST_QUEUE_NAME = "test-queue";
    private static final String TEST_ACCOUNT = System.getProperty("azure.accountName");
    public static final String TEST_QUEUE_CONTENT = "This is the queue!";

    @Override
    public void configure(CamelContext camelContext) {

        final String host = System.getProperty("azure.host");
        final String port = System.getProperty("azure.port");
        final StorageSharedKeyCredential credential =
                new StorageSharedKeyCredential(TEST_ACCOUNT, System.getProperty("azure.accountKey"));
        final String endpoint = "http://" + host + ":" + port + "/" + TEST_ACCOUNT;

        final QueueServiceClient queueServiceClient = new QueueServiceClientBuilder().endpoint(endpoint)
                .credential(credential)
                .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS).setPrettyPrintBody(true))
                .buildClient();

        final QueueConfiguration configuration = new QueueConfiguration();
        configuration.setServiceClient(queueServiceClient);
        configuration.setAccountName(TEST_ACCOUNT);
        configuration.setCredentials(credential);
        configuration.setCredentialType(SHARED_KEY_CREDENTIAL);

        final QueueComponent blobComponent = new QueueComponent();
        blobComponent.setConfiguration(configuration);

        camelContext.addComponent("azure-storage-queue", blobComponent);
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder -> builder.fromF("azure-storage-queue://%s/%s", TEST_ACCOUNT, TEST_QUEUE_NAME)
                .setHeader(QueueConstants.QUEUE_NAME, builder.constant(TEST_QUEUE_NAME))
                .log("Downloaded the blob with content: ${body}");
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.setHeader(QUEUE_OPERATION, builder.constant(QueueOperationDefinition.createQueue.name()))
                .setHeader(QueueConstants.QUEUE_NAME, builder.constant(TEST_QUEUE_NAME))
                .toF("azure-storage-queue://%s", TEST_ACCOUNT)
                .log("Created container successful: ${body}")
                .setHeader(QUEUE_OPERATION, builder.constant(QueueOperationDefinition.sendMessage.name()))
                .setHeader(QueueConstants.QUEUE_NAME, builder.constant(TEST_QUEUE_NAME))
                .setBody(builder.constant(TEST_QUEUE_CONTENT))
                .toF("azure-storage-queue://%s", TEST_ACCOUNT)
                .log("Uploaded blob successful: ${body}");
    }
}