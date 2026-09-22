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

import java.util.List;

import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

/**
 * Publishes a CXF endpoint using the default (OSGi Servlet Whiteboard) HTTP transport, so the test can verify that
 * the {@code camel-cxf-whiteboard} feature lets that transport coexist with another CXF HTTP transport
 * (Undertow, installed by the test itself) without conflict.
 */
@Component(
        name = "karaf-camel-cxf-whiteboard-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelCxfWhiteboardRouteSupplier implements CamelRouteSupplier {
    private static final String PORT_PATH = System.getProperty("cxf.whiteboard.port") + "/CamelCxfWhiteboardRouteSupplier";
    private static final String CXF_ENDPOINT_URI = "cxf://http://localhost:" + PORT_PATH
            + "/test?serviceClass=org.apache.karaf.camel.test.jaxws.HelloService";

    @Override
    public void createRoutes(RouteBuilder builder) {
        builder.from(CXF_ENDPOINT_URI).process(exchange -> {
            Message in = exchange.getIn();
            List<?> parameter = in.getBody(List.class);
            String operation = (String) in.getHeader(CxfConstants.OPERATION_NAME);
            exchange.getMessage().setBody(operation + " " + parameter.get(0));
        });
    }
}
