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
import org.apache.camel.component.chatscript.ChatScriptMessage;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.osgi.service.component.annotations.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component(
        name = "karaf-camel-chatscript-test",
        immediate = true,
        service = CamelChatscriptRouteSupplier.class
)
public class CamelChatscriptRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    public boolean consumerEnabled() {
        return false;
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        String g = "CS" + Math.random();
        ChatScriptMessage rqMsg = new ChatScriptMessage(g, "", "Hello");
        String rq = "";
        try {
            rq = new ObjectMapper().writeValueAsString(rqMsg);
        }  catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        configureConsumer(
                producerRoute.setBody(new SimpleExpression(rq))
                .toF("chatscript://localhost:%s/Harry?resetChat=true", System.getProperty("chatscript.port"))
                .log("received ${body}"));
    }

}

