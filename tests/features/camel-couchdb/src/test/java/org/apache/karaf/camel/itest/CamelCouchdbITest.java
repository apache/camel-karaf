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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.component.couchdb.CouchDbConstants;
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

import com.google.gson.JsonObject;

@CamelKarafTestHint(externalResourceProvider = CamelCouchdbITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelCouchdbITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    private static final int DB_ORIGINAL_PORT = 5984;
    private static final String COUCHDB_USER = "admin";
    private static final String COUCHDB_PASSWORD = "password";

    @Override
    public String getBodyToSend() {
        return """
            {
                "user": "Bob99", 
                "registered": "2009-09-03T09:13:18 -01:00",
                "status": "active"
            }
        """;
    }

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

        JsonObject event = ex.getIn().getBody(JsonObject.class);
        assertNotNull(event);
        assertEquals(event.get("id").getAsString(), ex.getIn().getHeader(CouchDbConstants.HEADER_DOC_ID, String.class));
        assertTrue(event.get("deleted").getAsBoolean());
    }

    public static final class ExternalResourceProviders {
        public static GenericContainerResource<CouchdbContainer> createCouchdbContainer() {

            CouchdbContainer couchdbContainer = new CouchdbContainer("couchdb:3.3.3");
            return new GenericContainerResource<>(couchdbContainer, resource -> {
                resource.setProperty("couchdb.host", couchdbContainer.getHost());
                resource.setProperty("couchdb.port",
                        Integer.toString(couchdbContainer.getMappedPort(DB_ORIGINAL_PORT)));
                resource.setProperty("couchdb.user", COUCHDB_USER);
                resource.setProperty("couchdb.pass", COUCHDB_PASSWORD);
            });
        }
    }

    public static class CouchdbContainer extends GenericContainer<CouchdbContainer> {

        public CouchdbContainer(final String containerName) {
            super(containerName);

            withExposedPorts(DB_ORIGINAL_PORT)
                .waitingFor(Wait.forListeningPort())
                .withEnv("COUCHDB_USER", COUCHDB_USER)
                .withEnv("COUCHDB_PASSWORD", COUCHDB_PASSWORD);
        }
    }
}