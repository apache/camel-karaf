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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "karaf-camel-bindy-test",
        immediate = true,
        service = CamelBindyRouteSupplier.class
)
public class CamelBindyRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    public static final String CSV = "1,B2,Keira,Knightley,ISIN,XX23456789,BUY,Share,400.25,EUR,14-01-2009,17-02-2011 23:21:59\r\n";

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {

        BindyCsvDataFormat camelDataFormat = new BindyCsvDataFormat(Order.class);
        camelDataFormat.setLocale("en");

        producerRoute.process(ex -> ex.getIn().setBody(CSV))
                .unmarshal(camelDataFormat)
                .marshal(camelDataFormat)
                .log("Decoded: ${body}")
                .toF("mock:%s", getResultMockName());
    }


}

