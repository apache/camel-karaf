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

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.apache.cxf.BusFactory;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureRouteITest;
import org.apache.karaf.camel.itests.AvailablePortProvider;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.apache.karaf.camel.test.beans.Customer;
import org.apache.karaf.camel.test.jaxws.HelloService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.OptionUtils.combine;

@CamelKarafTestHint(isBlueprintTest = true, externalResourceProvider = CamelCxfITest.ExternalResourceProviders.class,
        additionalRequiredFeatures = "camel-undertow")
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelCxfITest extends AbstractCamelSingleFeatureRouteITest {

    private static final String ECHO_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><ns1:echo xmlns:ns1=\"http://jaxws.test.camel.karaf.apache.org/\">"
            + "<arg0 xmlns=\"http://jaxws.test.camel.karaf.apache.org/\">Hello World!</arg0></ns1:echo></soap:Body></soap:Envelope>";
    private static final String TEST_MESSAGE = "Hello World!";

    private JAXBContext jaxb;
    private final HttpClient client = HttpClient.newHttpClient();

    @Before
    public void initCtx() throws JAXBException {
        this.jaxb = JAXBContext.newInstance(Customer.class);
    }

    @Test
    public void testCxfWs() throws Exception {
        testInvokingServiceFromCXFClient();
        testXmlDeclaration();
        testPublishEndpointUrl();
    }

    @Test
    public void testCxfRs() throws Exception {
        testGetCustomerOnlyHeaders();
        testNewCustomerWithQueryParam();
    }

    @Test
    public void testCxfRsBlueprint() throws Exception {
        testGetCustomerOnlyHeadersInBlueprint();
    }

    private String getPortPathRs() {
        return ExternalResourceProviders.getCxfRsPort() + "/CamelCxfRsRouteSupplier/rest";
    }

    private String getPortPathRsBlueprint() {
        return ExternalResourceProviders.getCxfRsBlueprintPort() + "/rss";
    }

    private String getWsEndpointAddress() {
        return "http://localhost:" + ExternalResourceProviders.getCxfWsPort() + "/CamelCxfWsRouteSupplier/test";
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

    private void testGetCustomerOnlyHeadersInBlueprint() throws Exception {
        URI uri = URI.create("http://localhost:%s/customerservice/customers/456".formatted(getPortPathRsBlueprint()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "text/xml")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Customer entity = (Customer) jaxb.createUnmarshaller().unmarshal(new StringReader(response.body()));
        assertEquals(456, entity.getId());
    }

    private void testGetCustomerOnlyHeaders() throws Exception {
        URI uri = URI.create("http://localhost:%s/customerservice/customers/123".formatted(getPortPathRs()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "text/xml")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Customer entity = (Customer) jaxb.createUnmarshaller().unmarshal(new StringReader(response.body()));
        assertEquals(123, entity.getId());
    }

    private void testNewCustomerWithQueryParam() throws Exception {
        URI uri = URI.create("http://localhost:%s/customerservice/customers?age=12".formatted(getPortPathRs()));
        StringWriter sw = new StringWriter();
        jaxb.createMarshaller().marshal(new Customer(123, "Raul"), sw);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "text/xml")
                .header("Accept", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(sw.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }


    private void testInvokingServiceFromCXFClient() throws Exception {
        ClientProxyFactoryBean proxyFactory = new ClientProxyFactoryBean();
        ClientFactoryBean clientBean = proxyFactory.getClientFactoryBean();
        clientBean.setAddress(getWsEndpointAddress());
        clientBean.setServiceClass(HelloService.class);
        clientBean.setBus(BusFactory.newInstance().createBus());

        HelloService helloService = (HelloService) proxyFactory.create();

        String result = helloService.echo(TEST_MESSAGE);
        assertEquals("We should get the echo string result from router", "echo " + TEST_MESSAGE, result);

        Boolean bool = helloService.echoBoolean(Boolean.TRUE);
        assertNotNull("The result should not be null", bool);
        assertEquals("We should get the echo boolean result from router", "true", bool.toString());
    }

    private void testXmlDeclaration() throws Exception {
        URI uri = URI.create(getWsEndpointAddress());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "text/xml; charset=UTF-8")
                .header("Accept", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(ECHO_REQUEST))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue("Can't find the xml declaration.", body.startsWith("<?xml version=\"1.0\" encoding="));
        assertTrue("The response content is incorrect.", body.contains("echo Hello World!"));
    }

    private void testPublishEndpointUrl() throws Exception {
        URI uri = URI.create(getWsEndpointAddress()+ "?wsdl");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "text/xml")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue("Can't find the right service location.", response.body().contains("http://www.simple.com/services/test"));
    }

    public static final class ExternalResourceProviders {
        public static final String CXF_RS_PORT = "cxf.rs.port";
        public static final String CXF_RS_BLUEPRINT_PORT = "cxf.rs.blueprint.port";
        public static final String CXF_WS_PORT = "cxf.ws.port";

        public static AvailablePortProvider createAvailablePortProvider() {
            return new AvailablePortProvider(List.of(CXF_RS_PORT, CXF_WS_PORT, CXF_RS_BLUEPRINT_PORT));
        }

        static String getCxfRsPort() {
            return System.getProperty(CXF_RS_PORT);
        }

        static String getCxfRsBlueprintPort() {
            return System.getProperty(CXF_RS_BLUEPRINT_PORT);
        }

        static String getCxfWsPort() {
            return System.getProperty(CXF_WS_PORT);
        }
    }
}