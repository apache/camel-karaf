/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.camel.test;

import static org.apache.camel.builder.Builder.constant;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.as2.AS2Component;
import org.apache.camel.component.as2.AS2Configuration;
import org.apache.camel.component.as2.api.AS2MediaType;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.camel.model.RouteDefinition;
import org.apache.hc.core5.http.ContentType;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;


@Component(
        name = "karaf-camel-as2-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelAs2RouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String REQUEST_URI = "/";
    private static final String SUBJECT = "Test Case";
    private static final String AS2_NAME = "878051556";
    private static final String FROM = "mrAS@example.org";

    private static final String EDI_MESSAGE = "UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'\n"
            + "UNH+00000000000117+INVOIC:D:97B:UN'\n"
            + "BGM+380+342459+9'\n"
            + "DTM+3:20060515:102'\n"
            + "RFF+ON:521052'\n"
            + "NAD+BY+792820524::16++CUMMINS MID-RANGE ENGINE PLANT'\n"
            + "NAD+SE+005435656::16++GENERAL WIDGET COMPANY'\n"
            + "CUX+1:USD'\n"
            + "LIN+1++157870:IN'\n"
            + "IMD+F++:::WIDGET'\n"
            + "QTY+47:1020:EA'\n"
            + "ALI+US'\n"
            + "MOA+203:1202.58'\n"
            + "PRI+INV:1.179'\n"
            + "LIN+2++157871:IN'\n"
            + "IMD+F++:::DIFFERENT WIDGET'\n"
            + "QTY+47:20:EA'\n"
            + "ALI+JP'\n"
            + "MOA+203:410'\n"
            + "PRI+INV:20.5'\n"
            + "UNS+S'\n"
            + "MOA+39:2137.58'\n"
            + "ALC+C+ABG'\n"
            + "MOA+8:525'\n"
            + "UNT+23+00000000000117'\n"
            + "UNZ+1+00000000000778'\n";
    private static final String EDI_MESSAGE_CONTENT_TRANSFER_ENCODING = "7bit";

    @Override
    public void configure(CamelContext context) {
        final int port = Integer.parseInt(System.getProperty("as2.port"));
        final AS2Configuration configuration = new AS2Configuration();
        configuration.setTargetHostname("localhost");
        configuration.setTargetPortNumber(port);
        configuration.setServerFqdn("example.com");
        configuration.setServerPortNumber(port);

        final AS2Component component = new AS2Component(context);
        component.setConfiguration(configuration);
        context.addComponent("as2", component);
    }


    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                builder.from("as2://server/listen?requestUriPattern=/")
                        .log("received message ${body}")
                        .setBody(constant("OK"));
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.setHeader("CamelAS2.requestUri", constant(REQUEST_URI))
                .setHeader("CamelAS2.subject", constant(SUBJECT))
                .setHeader("CamelAS2.from", constant(FROM))
                .setHeader("CamelAS2.as2From", constant(AS2_NAME))
                .setHeader("CamelAS2.as2To", constant(AS2_NAME))
                .setHeader("CamelAS2.as2MessageStructure", constant(AS2MessageStructure.PLAIN))
                .setHeader("CamelAS2.ediMessageContentType",
                        constant(ContentType.create(AS2MediaType.APPLICATION_EDIFACT, StandardCharsets.US_ASCII.name())))
                .setHeader("CamelAS2.ediMessageTransferEncoding", constant(EDI_MESSAGE_CONTENT_TRANSFER_ENCODING))
                .setHeader("CamelAS2.dispositionNotificationTo", constant(FROM))
                .setHeader("CamelAS2.attachedFileName", constant(""))
                .setBody(constant(EDI_MESSAGE))
                .to("as2:client/send?inBody=ediMessage")
                .log("message sent successfully");
    }

    @Override
    public void createRoutes(RouteBuilder builder) {
        //creating producer before consumer as in the parent class causes an http connection exception
        //so invert the order for this specific supplier
        configureConsumer(consumerRoute().apply(builder));
        configureProducer(builder,
                builder.fromF("direct:%s", getTestComponentName()).routeId("producer-%s".formatted(getTestComponentName())));
    }
}

