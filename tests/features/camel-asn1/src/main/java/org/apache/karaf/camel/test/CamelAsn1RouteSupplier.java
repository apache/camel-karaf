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

import java.util.function.Function;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.dataformat.ASN1DataFormat;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;


@Component(
        name = "karaf-camel-asn1-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelAsn1RouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        final ASN1DataFormat asn1 = new ASN1DataFormat();
        return builder ->
                builder.fromF("file:%s/test-classes/asn1_data?fileName=SMS_SINGLE.tt&noop=true",
                                System.getProperty("project.target"))
                        .unmarshal(asn1).marshal(asn1);
    }

    @Override
    protected boolean producerEnabled() {
        return false;
    }
}

