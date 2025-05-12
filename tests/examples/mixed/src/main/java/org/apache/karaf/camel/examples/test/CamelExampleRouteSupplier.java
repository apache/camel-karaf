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

package org.apache.karaf.camel.examples.test;

import org.apache.camel.builder.RouteBuilder;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "camel-karaf-examples-mixed-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelExampleRouteSupplier implements CamelRouteSupplier {
    @Override
    public void createRoutes(RouteBuilder builder) {
        builder.fromF("jetty:http://0.0.0.0:%s/example", System.getProperty("java.dsl.jetty.port"))
                .id("example-http-inbound")
                .convertBodyTo(String.class)
                .log("[EXAMPLE INBOUND] Received: ${body}")
                .choice()
                  .when().simple("${headers.CamelHttpMethod} == 'POST'")
                    .setHeader("type").jsonpath("$.notification.type")
                    .choice()
                      .when().simple("${header.type} == 'email'")
                        .log("[EXAMPLE INBOUND] Received email notification")
                        .to("direct:email")
                        .setHeader("Exchange.HTTP_RESPONSE_CODE", builder.constant(200))
                      .when().simple("${header.type} == 'http'")
                        .log("[EXAMPLE INBOUND] Received http notification")
                        .to("direct:http")
                        .setHeader("Exchange.HTTP_RESPONSE_CODE", builder.constant(200))
                      .otherwise()
                        .log("[EXAMPLE INBOUND] Unknown notification")
                        .setBody(builder.constant("{ \"status\": \"reject\", \"type\": \"unknown\" }"))
                        .setHeader("Exchange.HTTP_RESPONSE_CODE", builder.constant(400))
                      .end().endChoice()
                  .otherwise()
                    .log("[EXAMPLE INBOUND] only POST is accepted (${headers.CamelHttpMethod})")
                    .setBody(builder.constant("{ \"error\": \"only POST is accepted\" }"))
                    .setHeader("Exchange.HTTP_RESPONSE_CODE", builder.constant(500));

        builder.from("direct:email")
                .id("example-email")
                .log("[EXAMPLE EMAIL] Sending notification email")
                .setHeader("to").jsonpath("$.notification.to")
                .setHeader("subject", builder.constant("Notification"))
                .setHeader("payload").jsonpath("$.notification.message")
                //.to("smtp://localhost");
                .setBody(builder.simple("{ \"status\": \"email sent\", \"to\": \"${header.to}\", \"subject\": \"${header.subject}\" }"));

        builder.from("direct:http")
                .id("example-http")
                .log("[EXAMPLE HTTP] Sending http notification")
                .setHeader("service").jsonpath("$.notification.service")
                // send to HTTP service
                .setBody(builder.simple("{ \"status\": \"http requested\", \"service\": \"${header.service}\" }"));
    }
}
