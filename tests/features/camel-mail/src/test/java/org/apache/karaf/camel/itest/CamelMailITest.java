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
import org.apache.karaf.camel.itests.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.testcontainers.containers.FixedHostPortGenericContainer;

@CamelKarafTestHint(externalResourceProvider = CamelMailITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelMailITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK\r\n");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static class MailContainer extends FixedHostPortGenericContainer<MailContainer> {
        public MailContainer(String dockerImage) {
            super(dockerImage);
        }
    }

    public static final class ExternalResourceProviders {
        public static GenericContainerResource<MailContainer> createGreenMailContainer() {
            int smtpport = Utils.getNextAvailablePort();
            int pop3port = Utils.getNextAvailablePort(port -> port != smtpport);
            final MailContainer greenMailContainer =
                    new MailContainer("greenmail/standalone:2.0.1").withFixedExposedPort(smtpport, 3025)
                            .withFixedExposedPort(pop3port, 3110)
                            .withEnv("GREENMAIL_OPTS", "-Dgreenmail.users=camel:foo@localhost -Dgreenmail.setup.test.all -Dgreenmail.hostname=0.0.0.0 -Dgreenmail.auth.disabled");

            return new GenericContainerResource<>(greenMailContainer, resource -> {
                resource.setProperty("smtp.port", Integer.toString(smtpport));
                resource.setProperty("pop3.port", Integer.toString(pop3port));
            });
        }
    }
}