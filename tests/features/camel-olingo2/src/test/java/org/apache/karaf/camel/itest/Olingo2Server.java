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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.RuntimeCamelException;
import org.apache.karaf.camel.itests.AvailablePortProvider;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.commons.ODataHttpHeaders;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntityContainer;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmServiceMetadata;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.core.commons.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.HttpMethod;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

public class Olingo2Server extends AvailablePortProvider {

    public static final Logger LOG = LoggerFactory.getLogger(Olingo2Server.class);
    public static final String OLINGO_PORT = "olingo.port";

    private static MockWebServer server;
    private static Edm edm;
    private static EdmEntitySet manufacturersSet;
    private static final String MANUFACTURERS = "Manufacturers";
    private static final String METADATA = "$metadata";
    private static final String SERVICE_NAME = "MyFormula.svc";


    public Olingo2Server() {
        super(List.of(OLINGO_PORT));
    }

    @Override
    public void before() {
        super.before();
        try {
            initEdm();
            initServer();
        } catch (Exception e) {
            LOG.error("cannot init EDM", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void after() {
        try {
            server.close();
        } catch (IOException e) {
            LOG.error("cannot close server EDM", e);
        }
        super.after();
    }

    private static void initEdm() throws Exception {
        InputStream edmXml = Olingo2Server.class.getResourceAsStream("etag-enabled-service.xml");
        edm = EntityProvider.readMetadata(edmXml, true);
        assertNotNull(edm);

        EdmEntityContainer entityContainer = edm.getDefaultEntityContainer();
        assertNotNull(entityContainer);
        manufacturersSet = entityContainer.getEntitySet(MANUFACTURERS);
        assertNotNull(manufacturersSet);

        EdmEntityType entityType = manufacturersSet.getEntityType();
        assertNotNull(entityType);

        //
        // Check we have enabled eTag properties
        //
        EdmProperty property = (EdmProperty) entityType.getProperty("Id");
        assertNotNull(property.getFacets());
    }

    private void initServer() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {

            @SuppressWarnings("resource")
            @Override
            public MockResponse dispatch(RecordedRequest recordedRequest) {
                MockResponse mockResponse = new MockResponse();

                switch (recordedRequest.getMethod()) {
                case HttpMethod.GET:
                    try {
                        if (recordedRequest.getPath().endsWith("/" + MANUFACTURERS)) {
                            ODataResponse odataResponse = EntityProvider.writeFeed(ContentType.APPLICATION_JSON.toContentTypeString(),
                                    manufacturersSet, List.of(getEntityData()),
                                    EntityProviderWriteProperties.serviceRoot(getServiceUrl().uri()).build());
                            InputStream entityStream = odataResponse.getEntityAsStream();
                            mockResponse.setResponseCode(HttpStatusCodes.OK.getStatusCode());
                            mockResponse.setBody(new Buffer().readFrom(entityStream));
                            return mockResponse;
                        } else if (recordedRequest.getPath().endsWith("/" + METADATA)) {
                            EdmServiceMetadata serviceMetadata = edm.getServiceMetadata();
                            return mockResponse.setResponseCode(HttpStatusCodes.OK.getStatusCode())
                                    .addHeader(ODataHttpHeaders.DATASERVICEVERSION, serviceMetadata.getDataServiceVersion())
                                    .setBody(new Buffer().readFrom(serviceMetadata.getMetadata()));
                        }

                    } catch (Exception ex) {
                        throw new RuntimeCamelException(ex);
                    }
                    break;
                case HttpMethod.PATCH:
                case HttpMethod.PUT:
                case HttpMethod.POST:
                case HttpMethod.DELETE:
                    return mockResponse.setResponseCode(HttpStatusCodes.NO_CONTENT.getStatusCode());
                default:
                    break;
                }

                mockResponse.setResponseCode(HttpStatusCodes.NOT_FOUND.getStatusCode()).setBody("{ status: \"Not Found\"}");
                return mockResponse;
            }
        });
        server.start(Integer.parseInt(super.properties().get(OLINGO_PORT)));
    }

    protected static Map<String, Object> getEntityData() {
        Map<String, Object> data = new HashMap<>();
        data.put("Id", "123");
        data.put("Name", "MyCarManufacturer");
        data.put("Founded", new Date());
        Map<String, Object> address = new HashMap<>();
        address.put("Street", "Main");
        address.put("ZipCode", "42421");
        address.put("City", "Fairy City");
        address.put("Country", "FarFarAway");
        data.put("Address", address);
        return data;
    }

    private static HttpUrl getServiceUrl() {
        if (server == null) {
            LOG.error("Test programming failure. Server not initialised");
        }
        return server.url(SERVICE_NAME);
    }

}
