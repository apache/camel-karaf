/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.karaf.camel.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletRequest;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.cxf.endpoint.Client;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import static org.apache.camel.builder.Builder.header;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Component(
        name = "karaf-camel-cxf-ws-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelCxfWsRouteSupplier implements CamelRouteSupplier {
    private static final String PORT_WS_PATH = System.getProperty("cxf.ws.port") + "/CamelCxfWsRouteSupplier";
    private static final String CXF_WS_ENDPOINT_URI = "cxf://http://localhost:" + PORT_WS_PATH
            + "/test?serviceClass=org.apache.karaf.camel.test.jaxws.HelloService"
            + "&publishedEndpointUrl=http://www.simple.com/services/test";

    private static final String ECHO_OPERATION = "echo";
    private static final String ECHO_BOOLEAN_OPERATION = "echoBoolean";

    @Override
    public void createRoutes(RouteBuilder builder) {
        builder.from(CXF_WS_ENDPOINT_URI).choice().when(header(CxfConstants.OPERATION_NAME).isEqualTo(ECHO_OPERATION))
                .process(exchange -> {
                    assertEquals(DataFormat.POJO,
                            exchange.getProperty(CxfConstants.DATA_FORMAT_PROPERTY, DataFormat.class));
                    Message in = exchange.getIn();
                    // check the remote IP from the cxfMessage
                    org.apache.cxf.message.Message cxfMessage
                            = in.getHeader(CxfConstants.CAMEL_CXF_MESSAGE, org.apache.cxf.message.Message.class);
                    assertNotNull("Should get the cxfMessage instance from message header", cxfMessage);
                    ServletRequest request = (ServletRequest) cxfMessage.get("HTTP.REQUEST");
                    assertNotNull("Should get the ServletRequest", request);
                    assertNotNull("Should get the RemoteAddress", request.getRemoteAddr());
                    // Could verify the HttpRequest
                    String contentType = in.getHeader(Exchange.CONTENT_TYPE, String.class);
                    assertNotNull("Should get the contentType.", contentType);

                    // Get the parameter list
                    List<?> parameter = in.getBody(List.class);
                    // Get the operation name
                    String operation = (String) in.getHeader(CxfConstants.OPERATION_NAME);
                    Object result = operation + " " + parameter.get(0);
                    // Put the result back
                    exchange.getMessage().setBody(result);
                    // set up the response context which force start document
                    Map<String, Object> map = new HashMap<>();
                    map.put("org.apache.cxf.stax.force-start-document", Boolean.TRUE);
                    exchange.getMessage().setHeader(Client.RESPONSE_CONTEXT, map);
                })
                .when(header(CxfConstants.OPERATION_NAME).isEqualTo(ECHO_BOOLEAN_OPERATION)).process(exchange -> {
                    Message in = exchange.getIn();
                    // Get the parameter list
                    List<?> parameter = in.getBody(List.class);
                    // Put the result back
                    exchange.getMessage().setBody(parameter.get(0));
                });
    }
}
