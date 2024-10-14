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


import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import java.util.HashMap;
import java.util.Map;

import static org.apache.camel.builder.Builder.constant;

@Component(name = "karaf-camel-influxdb2-test", immediate = true, service = CamelRouteSupplier.class)
public class CamelInfluxdb2RouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String ORG = System.getProperty("influxdb2.org");
    private static final String BUCKET = System.getProperty("influxdb2.bucket");

    @Override
    public void configure(CamelContext camelContext) {
        final int influxdb2Port = Integer.getInteger("influxdb2.port");
        InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://localhost:%s".formatted(influxdb2Port),
                System.getProperty("influxdb2.admin.token").toCharArray(), ORG, BUCKET);

        camelContext.getRegistry().bind("myDbClient", influxDBClient);
    }

    @Override
    public void cleanUp(CamelContext camelContext) {
        InfluxDBClient influxDBClient = camelContext.getRegistry().lookupByNameAndType("myDbClient", InfluxDBClient.class);
        if (influxDBClient == null) {
            return;
        }
        influxDBClient.close();
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute
                .log("calling influxdb2")
                .toF("influxdb2:myDbClient?operation=ping&org=%s&bucket=%s&autoCreateBucket=true", ORG, BUCKET)
                .setBody(builder.constant("OK_PING"))
                .log("Ping response: ${body}")
                .toF("mock:%s", getResultMockName())
                .log("calling to write to influxdb2")
                .setBody(constant(getSampleWriteData()))
                .toF("influxdb2:myDbClient?operation=insert&org=%s&bucket=%s&autoCreateBucket=true", ORG, BUCKET)
                .setBody(builder.constant("OK_INSERT"))
                .log("Insert response: ${body}")
                .toF("mock:%s", getResultMockName());
    }

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    private Map<String, String> getSampleWriteData() {
        Map<String, String> dbData = new HashMap<>();
        dbData.put("CamelInfluxDB2MeasurementName", "cpu");
        dbData.put("time", "1307816001290");
        dbData.put("idle", "90L");
        dbData.put("user", "9L");

        return dbData;
    }
}