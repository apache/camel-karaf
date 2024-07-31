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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.function.Consumer;

import org.apache.camel.Exchange;
import org.apache.camel.component.chatscript.ChatScriptMessage;
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

@CamelKarafTestHint(externalResourceProvider = CamelChatscriptITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelChatscriptITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedMessageCount(1);
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
        MockEndpoint endpoint = getMockEndpoint();
        List<Exchange> exchanges = endpoint.getExchanges();
        assertNotNull(exchanges);
        assertEquals(1, exchanges.size());
        Exchange ex = exchanges.get(0);

        ChatScriptMessage event = ex.getIn().getBody(ChatScriptMessage.class);
        assertFalse(event.getReply().isEmpty());
    }

    public static final class ExternalResourceProviders {

        private static final int PORT_DEFAULT = 1024;

        public static GenericContainerResource createArangoContainer() {
            final GenericContainer<?> chatscriptContainer =
                    new GenericContainer<>("claytantor/chatscript-docker:latest")
                            .withExposedPorts(PORT_DEFAULT);

            return new GenericContainerResource(chatscriptContainer, (Consumer<GenericContainerResource>) resource -> {
                resource.setProperty("chatscript.port", Integer.toString(chatscriptContainer.getMappedPort(PORT_DEFAULT)));
            });
        }
    }
}