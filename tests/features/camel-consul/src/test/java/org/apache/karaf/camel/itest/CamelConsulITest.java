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
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.GenericContainerResource;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwiproject.consul.Consul;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@CamelKarafTestHint(externalResourceProvider = CamelConsulITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelConsulITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {

        public static GenericContainerResource createConsulContainer() {
            final GenericContainer<?> consulContainer =
                    new GenericContainer<>("hashicorp/consul:1.19")
                            .withExposedPorts(Consul.DEFAULT_HTTP_PORT)
                            .waitingFor(Wait.forLogMessage(".*Synced node info.*", 1))
                            .withCommand("agent", "-dev", "-server", "-bootstrap", "-client", "0.0.0.0", "-log-level", "trace");

            return new GenericContainerResource(consulContainer, (Consumer<GenericContainerResource>) resource -> {
                resource.setProperty("consul.port", Integer.toString(consulContainer.getMappedPort(Consul.DEFAULT_HTTP_PORT)));
            });
        }
    }
}