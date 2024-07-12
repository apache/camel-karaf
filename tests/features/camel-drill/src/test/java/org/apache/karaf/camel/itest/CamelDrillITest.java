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

@CamelKarafTestHint(externalResourceProvider = CamelDrillITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelDrillITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    private static final int ORIGINAL_JDBC_PORT = 31010;

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {
        public static GenericContainerResource<DrillContainer> createDrillStorageBlobContainer() {

            DrillContainer drillContainer = new DrillContainer("apache/drill:1.21.1");
            return new GenericContainerResource<>(drillContainer, resource -> {
                resource.setProperty("drill.host", drillContainer.getHost());
                resource.setProperty("drill.port", Integer.toString(drillContainer.getMappedPort(ORIGINAL_JDBC_PORT)));
            });
        }
    }

    public static class DrillContainer extends GenericContainer<DrillContainer> {

        public DrillContainer(final String containerName) {
            super(containerName);

            withExposedPorts(ORIGINAL_JDBC_PORT)
                .waitingFor(Wait.forListeningPort())
                // prevent the container from stopping prematurely in non interactive sessions
                // see https://github.com/apache/drill/issues/2829 for more details
                .withCreateContainerCmdModifier(cmd -> cmd.withTty(true));
        }
    }
}