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
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "karaf-camel-csv-test",
        immediate = true,
        service = CamelCsvRouteSupplier.class
)
public class CamelCsvRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        CsvDataFormat csv = new CsvDataFormat();
        csv.setDelimiter(",");
        csv.setRecordSeparator("\n");
        csv.setSkipHeaderRecord("false");
        csv.setTrim("false");
        csv.setUseMaps("false");
        csv.setQuoteDisabled("true");

        producerRoute.log("Will unmarshal: ${body}")
                    .unmarshal(csv)
                    .log("Unmarshal: ${body}")
                    .toF("mock:%s", getResultMockName())
                    .log("Will marshal: ${body}")
                    .marshal(csv)
                    .log("Marshal: ${body}")
                    .toF("mock:%s", getResultMockName());
    }
}