/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.camel.itest;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.GenericContainerResource;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import static org.apache.karaf.camel.test.CamelAzureStorageQueueRouteSupplier.TEST_QUEUE_CONTENT;

@CamelKarafTestHint(externalResourceProvider = CamelAzureStorageQueueITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelAzureStorageQueueITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    private static final int AZURITE_ORIGINAL_PORT = 10001;

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived(TEST_QUEUE_CONTENT);
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {
        private static final String DEFAULT_ACCOUNT_NAME = "devstoreaccount1";
        private static final String DEFAULT_ACCOUNT_KEY
                = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

        public static GenericContainerResource<AzuriteContainer> createAzureStorageQueueContainer() {

            AzuriteContainer azuriteContainer = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.31.0");

            return new GenericContainerResource<>(azuriteContainer, resource -> {
                resource.setProperty("azure.host", azuriteContainer.getHost());
                resource.setProperty("azure.port",
                        Integer.toString(azuriteContainer.getMappedPort(AZURITE_ORIGINAL_PORT)));
                resource.setProperty("azure.accountName", DEFAULT_ACCOUNT_NAME);
                resource.setProperty("azure.accountKey", DEFAULT_ACCOUNT_KEY);
            });
        }
    }

    public static class AzuriteContainer extends GenericContainer<AzuriteContainer> {

        public AzuriteContainer(final String containerName) {
            super(containerName);

            withExposedPorts(AZURITE_ORIGINAL_PORT)
                    .waitingFor(Wait.forListeningPort());
        }

    }
}