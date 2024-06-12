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
import org.testcontainers.activemq.ActiveMQContainer;

@CamelKarafTestHint(
        externalResourceProvider = CamelActivemqITest.ExternalResourceProviders.class,
        isBlueprintTest = true)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelActivemqITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {
        
        public static final String ACTIVEMQ_USER = "myUser";
        public static final String ACTIVEMQ_PASSWORD = "myPassword";
        public static final int ACTIVEMQ_ORIGINAL_PORT = 61616;

        public static GenericContainerResource<ActiveMQContainer> createActiveMQContainer() {

            ActiveMQContainer activemqContainer = new ActiveMQContainer("apache/activemq-classic:6.1.0")
                    .withUser(ACTIVEMQ_USER)
                    .withPassword(ACTIVEMQ_PASSWORD);

            return new GenericContainerResource<>(activemqContainer,
                    resource -> {
                        resource.setProperty("activemq.host", activemqContainer.getHost());
                        resource.setProperty("activemq.port", Integer.toString(activemqContainer.getMappedPort(ACTIVEMQ_ORIGINAL_PORT)));
                        resource.setProperty("activemq.username", ACTIVEMQ_USER);
                        resource.setProperty("activemq.password", ACTIVEMQ_PASSWORD);
                    }
            );
        }
    }
}