/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.karaf.component.vm;

import org.apache.camel.*;
import org.apache.camel.component.seda.SedaConsumer;
import org.apache.camel.support.DefaultExchange;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class VmConsumer extends SedaConsumer implements CamelContextAware {

    private CamelContext camelContext;

    public VmConsumer(VmEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    /**
     * Strategy to prepare exchange for being processed by this consumer
     *
     * @param exchange the exchange
     * @return the exchange to process by this consumer
     */
    @Override
    protected Exchange prepareExchange(Exchange exchange) {
        // send a new copy exchange with the camel context from this consumer
        Exchange newExchange = new DefaultExchange(getCamelContext(), exchange.getPattern());
        if (exchange.hasProperties()) {
            newExchange.getExchangeExtension().setProperties(safeCopyProperties(exchange.getProperties()));
        }
        exchange.getExchangeExtension().copyInternalProperties(newExchange);
        // safe copy message history using a defensive copy
        List<MessageHistory> history = (List<MessageHistory>) exchange.getProperty(ExchangePropertyKey.MESSAGE_HISTORY);
        if (history != null) {
            // use thread-safe list as message history may be accessed concurrently
            newExchange.setProperty(ExchangePropertyKey.MESSAGE_HISTORY, new CopyOnWriteArrayList<>(history));
        }
        // no handover
        // exchange.getExchangeExtension().handoverCompletions(newExchange);
        newExchange.setIn(exchange.getIn().copy());
        if (exchange.hasOut()) {
            newExchange.setOut(exchange.getOut().copy());
        }
        newExchange.setException(exchange.getException());
        // this consumer grabbed the exchange so mark its from this route/endpoint
        newExchange.getExchangeExtension().setFromEndpoint(getEndpoint());
        newExchange.getExchangeExtension().setFromRouteId(getRouteId());
        return newExchange;
    }

    private static Map<String, Object> safeCopyProperties(Map<String, Object> properties) {
        if (properties == null) {
            return null;
        }
        return new ConcurrentHashMap<>(properties);
    }

}
