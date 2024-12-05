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

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.apache.karaf.camel.test.beans.Customer;
import org.osgi.service.component.annotations.Component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Component(
        name = "karaf-camel-cxf-rs-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelCxfRsRouteSupplier implements CamelRouteSupplier {
    private static final String PORT_RS_PATH = System.getProperty("cxf.rs.port") + "/CamelCxfRsRouteSupplier";
    private static final String CXF_RS_ENDPOINT_URI = "cxfrs://http://localhost:" + PORT_RS_PATH
            + "/rest?resourceClasses=org.apache.karaf.camel.test.beans.CustomerService&bindingStyle=SimpleConsumer";


    @Override
    public void createRoutes(RouteBuilder builder) {
        builder.from(CXF_RS_ENDPOINT_URI)
                .recipientList(builder.simple("direct:${header.operationName}"));

        builder.from("direct:getCustomer").process(exchange -> {
            assertEquals("123", exchange.getIn().getHeader("id"));
            exchange.getMessage().setBody(new Customer(123, "Raul"));
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        });

        builder.from("direct-vm:getCustomer").process(exchange -> {
            assertEquals("456", exchange.getIn().getHeader("id"));
            exchange.getMessage().setBody(new Customer(456, "John"));
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        });

        builder.from("direct:newCustomer").process(exchange -> {
            Customer c = exchange.getIn().getBody(Customer.class);
            assertNotNull(c);
            assertEquals(123, c.getId());
            assertEquals(12, exchange.getIn().getHeader("age"));
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        });
    }
}
