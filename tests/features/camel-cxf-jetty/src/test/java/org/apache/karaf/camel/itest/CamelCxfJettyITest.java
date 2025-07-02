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

import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureRouteITest;
import org.apache.karaf.camel.itests.AvailablePortProvider;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@CamelKarafTestHint(isBlueprintTest = true, externalResourceProvider = CamelCxfJettyITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelCxfJettyITest extends AbstractCamelSingleFeatureRouteITest {

    private static final String ECHO_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soap:Body><ns1:echo xmlns:ns1=\"http://jaxws.test.camel.karaf.apache.org/\">" +
            "<arg0 xmlns=\"http://jaxws.test.camel.karaf.apache.org/\">Hello World!</arg0></ns1:echo>" +
            "</soap:Body></soap:Envelope>";

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    public void testCxfJetty() throws Exception {
        testEchoOperation();
    }

    private String getCxfJettyEndpointAddress() {
        return "http://localhost:" + ExternalResourceProviders.getCxfJettyBlueprintPort() + "/cxfjetty";
    }

    private void testEchoOperation() throws Exception {
        URI uri = URI.create(getCxfJettyEndpointAddress());
        HttpRequest request = HttpRequest.newBuilder().uri(uri).header("Content-Type", "text/xml; charset=UTF-8").header("Accept", "text/xml").POST(HttpRequest.BodyPublishers.ofString(ECHO_REQUEST)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue("The response content is incorrect.", body.contains("echo Hello World!"));
    }

    public static final class ExternalResourceProviders {
        public static final String CXF_JETTY_BLUEPRINT_PORT = "cxf.jetty.blueprint.port";

        public static AvailablePortProvider createAvailablePortProvider() {
            return new AvailablePortProvider(List.of(CXF_JETTY_BLUEPRINT_PORT));
        }

        static String getCxfJettyBlueprintPort() {
            return System.getProperty(CXF_JETTY_BLUEPRINT_PORT);
        }
    }
}