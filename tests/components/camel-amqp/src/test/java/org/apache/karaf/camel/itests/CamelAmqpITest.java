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
package org.apache.karaf.camel.itests;

import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.testcontainers.containers.RabbitMQContainer;


@UseExternalResourceProvider(CamelAmqpITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelAmqpITest extends AbstractCamelSingleComponentResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    @Override
    public boolean isBlueprintTest() {
        return true;
    }

    public static final class ExternalResourceProviders {
        private static final String PASSWORD = "s3cret";

        public static GenericContainerResource<RabbitMQContainer> createRabbitMQContainer() {
            final RabbitMQContainer rabbitMQContainer =
                    new RabbitMQContainer("rabbitmq:3.13.1")
                            .withAdminPassword(PASSWORD);
            return new GenericContainerResource<>(rabbitMQContainer,
                    resource -> {
                        try {
                            rabbitMQContainer.execInContainer("rabbitmq-plugins", "enable", "rabbitmq_amqp1_0");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        resource.setProperty("amqp.host", rabbitMQContainer.getHost());
                        resource.setProperty("amqp.port", Integer.toString(rabbitMQContainer.getAmqpPort()));
                        resource.setProperty("amqp.username", rabbitMQContainer.getAdminUsername());
                        resource.setProperty("amqp.password", rabbitMQContainer.getAdminPassword());
                    }
            );
        }
    }
}