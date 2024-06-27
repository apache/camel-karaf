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
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.testcontainers.containers.GenericContainer;

@CamelKarafTestHint(externalResourceProvider = CamelArangodbITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelArangodbITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    public static final Integer PORT_DEFAULT = 8529;

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }


    public static final class ExternalResourceProviders {

        public static GenericContainerResource createArangoContainer() {
            final GenericContainer<?> arangoContainer =
                    new GenericContainer<>("arangodb/arangodb:3.11.5")
                            .withExposedPorts(PORT_DEFAULT)
                            .withEnv("ARANGO_NO_AUTH", "1");

            return new GenericContainerResource(arangoContainer, (Consumer<GenericContainerResource>) resource -> {
                resource.setProperty("arango.port", Integer.toString(arangoContainer.getMappedPort(PORT_DEFAULT)));
            });
        }
    }
}