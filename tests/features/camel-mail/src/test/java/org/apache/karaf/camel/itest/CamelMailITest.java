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
import org.testcontainers.containers.GenericContainer;

@CamelKarafTestHint(externalResourceProvider = CamelMailITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelMailITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    private static final int SMTP_PORT = 3025;
    private static final int POP3_PORT = 3110;

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK\r\n");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {

        public static GenericContainerResource createGreenMailContainer() {
            final GenericContainer<?> greenMailContainer =
                    new GenericContainer<>("greenmail/standalone:2.0.1")
                            .withExposedPorts(SMTP_PORT, POP3_PORT)
                            .withEnv("GREENMAIL_OPTS", "-Dgreenmail.users=camel:foo@localhost -Dgreenmail.setup.test.all -Dgreenmail.hostname=0.0.0.0 -Dgreenmail.auth.disabled");

            return new GenericContainerResource(greenMailContainer, (Consumer<GenericContainerResource>) resource -> {
                resource.setProperty("smtp.port", Integer.toString(greenMailContainer.getMappedPort(SMTP_PORT)));
                resource.setProperty("pop3.port", Integer.toString(greenMailContainer.getMappedPort(POP3_PORT)));
            });
        }
    }
}