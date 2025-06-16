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

import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.GenericContainerResource;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.testcontainers.azure.EventHubsEmulatorContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.azure.AzuriteContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@CamelKarafTestHint(externalResourceProvider = CamelAzureEventhubsITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelAzureEventhubsITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    public static final int EVENTHUBS_EMULATOR_PORT = 5672;

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {
        private static final String DEFAULT_ACCOUNT_NAME = "devstoreaccount1";
        private static final String DEFAULT_ACCOUNT_KEY
                = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
        private static final int AZURITE_ORIGINAL_PORT =  10000;

        private static final Network network = Network.newNetwork();
        private static final AzuriteContainer azuriteContainer =
                new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")
                        .withNetwork(network)
                        .withNetworkAliases("azurite")
                        .waitingFor(Wait.forListeningPort());

        public static GenericContainerResource<AzuriteContainer> createAzureStorageContainer() {

            return new GenericContainerResource<AzuriteContainer>(azuriteContainer, resource -> {
                resource.setProperty("azure.host", azuriteContainer.getHost());
                resource.setProperty("azure.port",
                        Integer.toString(azuriteContainer.getMappedPort(AZURITE_ORIGINAL_PORT)));
                resource.setProperty("azure.accountName", DEFAULT_ACCOUNT_NAME);
                resource.setProperty("azure.accountKey", DEFAULT_ACCOUNT_KEY);
            });
        }

        public static GenericContainerResource<EventHubsEmulatorContainer> createAzureEventHubsContainer() {
            EventHubsEmulatorContainer eventHubContainer =
                    new EventHubsEmulatorContainer("mcr.microsoft.com/azure-messaging/eventhubs-emulator:2.0.1")
                            .withNetwork(network).withNetworkAliases("eventhubs-emulator").withAzuriteContainer(azuriteContainer)
                            .withExposedPorts(EVENTHUBS_EMULATOR_PORT)
                            .withEnv("ACCEPT_EULA", "Y");

            return new GenericContainerResource<>(eventHubContainer, resource -> {
                resource.setProperty("azure.connectionString", eventHubContainer.getConnectionString());
            });
        }
    }
}