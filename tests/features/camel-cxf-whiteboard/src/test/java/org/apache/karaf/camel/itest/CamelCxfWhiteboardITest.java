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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureRouteITest;
import org.apache.karaf.camel.itests.AvailablePortProvider;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * Verifies that the {@code camel-cxf-whiteboard} feature (base {@code camel-cxf} plus {@code http-whiteboard})
 * installs cleanly and doesn't conflict with a CXF endpoint actually served over another transport (Undertow,
 * installed here exactly like {@code CamelCxfITest} does) - i.e. that pulling in the OSGi Servlet Whiteboard
 * registration from {@code camel-cxf-all} is safe to combine with a real HTTP transport.
 */
@CamelKarafTestHint(additionalRequiredFeatures = "camel-undertow",
        externalResourceProvider = CamelCxfWhiteboardITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelCxfWhiteboardITest extends AbstractCamelSingleFeatureRouteITest {

    private static final String ECHO_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><ns1:echo xmlns:ns1=\"http://jaxws.test.camel.karaf.apache.org/\">"
            + "<arg0 xmlns=\"http://jaxws.test.camel.karaf.apache.org/\">Hello World!</arg0></ns1:echo></soap:Body></soap:Envelope>";

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    public void testCxfWhiteboard() throws Exception {
        URI uri = URI.create(getWsEndpointAddress());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "text/xml; charset=UTF-8")
                .header("Accept", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(ECHO_REQUEST))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue("The response content is incorrect.", response.body().contains("echo Hello World!"));
    }

    private String getWsEndpointAddress() {
        return "http://localhost:" + System.getProperty(ExternalResourceProviders.CXF_WHITEBOARD_PORT) + "/CamelCxfWhiteboardRouteSupplier/test";
    }

    @Override
    protected List<String> installRequiredBundles() throws Exception {
        List<String> bundles = new ArrayList<>();
        // Using the wrap protocol to install the bundle with Import-Package=* to avoid the issue with the bad
        // jakarta.xml.bind and jakarta.servlet import version ranges
        installBundle("wrap:mvn:org.apache.cxf/cxf-rt-transports-http-undertow/%s$overwrite=merge&Import-Package=*".formatted(System.getProperty("cxf-version")), true);
        String undertowTransport = "org.apache.cxf.cxf-rt-transports-http-undertow";
        assertBundleInstalledAndRunning(undertowTransport);
        bundles.add(undertowTransport);
        bundles.addAll(super.installRequiredBundles());
        return bundles;
    }

    @Override
    protected Option[] getAdditionalOptions() {
        return combine(
            super.getAdditionalOptions(), CoreOptions.systemProperty("cxf-version").value(System.getProperty("cxf.version"))
        );
    }

    public static final class ExternalResourceProviders {
        public static final String CXF_WHITEBOARD_PORT = "cxf.whiteboard.port";

        public static AvailablePortProvider createAvailablePortProvider() {
            return new AvailablePortProvider(List.of(CXF_WHITEBOARD_PORT));
        }
    }
}
