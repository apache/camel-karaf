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

import java.util.function.Consumer;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@CamelKarafTestHint(externalResourceProvider = CamelPahoMqtt5ITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelPahoMqtt5ITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    private static final int CONTAINER_PORT = 1883;

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {

        public static GenericContainerResource createMqttContainer() {
            final GenericContainer<?> mqttContainer =
                    new GenericContainer<>("eclipse-mosquitto:2.0.18")
                            .withExposedPorts(CONTAINER_PORT);

            mqttContainer.withNetworkAliases("mosquitto")
                    .withClasspathResourceMapping("mosquitto.conf", "/mosquitto/config/mosquitto.conf", BindMode.READ_ONLY)
                    .waitingFor(Wait.forLogMessage(".*mosquitto version 2.0.18 running.*", 1));

            return new GenericContainerResource(mqttContainer, (Consumer<GenericContainerResource>) resource -> {
                resource.setProperty("mqtt.port", Integer.toString(mqttContainer.getMappedPort(CONTAINER_PORT)));
            });
        }
    }
}