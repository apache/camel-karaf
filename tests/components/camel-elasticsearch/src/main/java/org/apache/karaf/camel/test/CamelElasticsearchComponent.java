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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.es.ElasticsearchComponent;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelComponentResultMockBased;
import org.osgi.service.component.annotations.Component;

@Component(name = "karaf-camel-elasticsearch-test", immediate = true)
public class CamelElasticsearchComponent extends AbstractCamelComponentResultMockBased {

    private static final String INDEX_NAME = "testindex";

    @Override
    protected void configureCamelContext(CamelContext camelContext) {
        final ElasticsearchComponent elasticsearchComponent = new ElasticsearchComponent();
        elasticsearchComponent.setEnableSSL(true);
        elasticsearchComponent.setHostAddresses(
            "%s:%s".formatted(System.getProperty("elasticsearch.host"), System.getProperty("elasticsearch.port"))
        );
        elasticsearchComponent.setUser(System.getProperty("elasticsearch.username"));
        elasticsearchComponent.setPassword(System.getProperty("elasticsearch.password"));
        elasticsearchComponent.setCertificatePath("file:%s".formatted(System.getProperty("elasticsearch.cafile")));

        camelContext.addComponent("elasticsearch",elasticsearchComponent);
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        //to add the mock endpoint at the end of the route, call configureConsumer
        configureConsumer(
            producerRoute.toF("elasticsearch://elasticsearch?operation=Exists&indexName=%s", INDEX_NAME)
                    .log("Index exist: ${body}")
                    .setBody(builder.simple("""
                            {"date": "${header.CamelTimerFiredTime}", "someKey": "someValue"}
                            """))
                    .toF("elasticsearch://elasticsearch?operation=Index&indexName=%s", INDEX_NAME)
                    .log("Index doc : ${body}")
                    .setHeader("_ID", builder.simple("${body}"))
                    .toF("elasticsearch://elasticsearch?operation=GetById&indexName=%s", INDEX_NAME)
                    .log("Get doc: ${body}")
                    .setHeader("indexId", builder.simple("${header._ID}"))
                    .setBody(builder.constant("""
                            {"doc": {"someKey": "someValue2"}}
                            """))
                    .toF("elasticsearch://elasticsearch?operation=Update&indexName=%s", INDEX_NAME)
                    .log("Update doc: ${body} ")
                    .setBody(builder.simple("${header._ID}"))
                    .toF("elasticsearch://elasticsearch?operation=GetById&indexName=%s", INDEX_NAME)
                    .log("Get doc: ${body}")
                    .setBody(builder.simple("${header._ID}"))
                    .toF("elasticsearch://elasticsearch?operation=Delete&indexName=%s", INDEX_NAME)
                    .log("Delete doc: ${body}")
                    .setBody(builder.constant("OK"))
        );

    }

    @Override
    protected boolean consumerEnabled() {
        return false;
    }
}