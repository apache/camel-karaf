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

import static org.apache.camel.component.azure.storage.blob.BlobConstants.BLOB_CONTAINER_NAME;
import static org.apache.camel.component.azure.storage.blob.BlobConstants.BLOB_NAME;
import static org.apache.camel.component.azure.storage.blob.BlobConstants.BLOB_OPERATION;
import static org.apache.camel.component.azure.storage.blob.CredentialType.SHARED_KEY_CREDENTIAL;

import java.util.function.Function;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobComponent;
import org.apache.camel.component.azure.storage.blob.BlobConfiguration;
import org.apache.camel.component.azure.storage.blob.BlobOperationsDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;

@Component(name = "karaf-camel-azure-storage-blob-test", immediate = true, service = CamelRouteSupplier.class)
public class CamelAzureStorageBlobRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String TEST_BLOB_CONTAINER_NAME = "test-container";
    private static final String TEST_BLOB_NAME = "hello.txt";

    @Override
    public void configure(CamelContext camelContext) {
        final String accountName = System.getProperty("azure.accountName");
        final String host = System.getProperty("azure.host");
        final String port = System.getProperty("azure.port");
        final StorageSharedKeyCredential credential =
                new StorageSharedKeyCredential(accountName, System.getProperty("azure.accountKey"));
        final String endpoint = "http://" + host + ":" + port + "/" + accountName;

        final BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().endpoint(endpoint)
                .credential(credential)
                .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS).setPrettyPrintBody(true))
                .buildClient();

        final BlobConfiguration configuration = new BlobConfiguration();
        configuration.setServiceClient(blobServiceClient);
        configuration.setAccountName(accountName);
        configuration.setCredentials(credential);
        configuration.setCredentialType(SHARED_KEY_CREDENTIAL);

        final BlobComponent blobComponent = new BlobComponent();
        blobComponent.setConfiguration(configuration);

        camelContext.addComponent("azure-storage-blob", blobComponent);
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder -> builder.from("azure-storage-blob://devstoreaccount1/" + TEST_BLOB_CONTAINER_NAME + "?blobName=" + TEST_BLOB_NAME)
                .setHeader(BLOB_CONTAINER_NAME, builder.constant(TEST_BLOB_CONTAINER_NAME))
                .setHeader(BLOB_NAME, builder.constant(TEST_BLOB_NAME))
                .log("Downloaded the blob with content: ${body}")
                .setBody(builder.constant("OK"));
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.setHeader(BLOB_OPERATION, builder.constant(BlobOperationsDefinition.createBlobContainer.name()))
                .setHeader(BLOB_CONTAINER_NAME, builder.constant(TEST_BLOB_CONTAINER_NAME))
                .toF("azure-storage-blob://devstoreaccount1")
                .log("Created container successful: ${body}")
                .setHeader(BLOB_OPERATION, builder.constant(BlobOperationsDefinition.uploadBlockBlob.name()))
                .setHeader(BLOB_NAME, builder.constant(TEST_BLOB_NAME))
                .setBody(builder.constant("This is the blob!"))
                .toF("azure-storage-blob://devstoreaccount1")
                .log("Uploaded blob successful: ${body}");
    }
}