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

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangeExtension;
import org.apache.camel.Processor;
import org.apache.camel.component.seda.SedaConsumer;
import org.apache.camel.support.ExchangeHelper;

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

    @Override
    protected Exchange prepareExchange(Exchange exchange) {
        Exchange result = ExchangeHelper.copyExchangeWithProperties(exchange, camelContext);
        exchange.getExchangeExtension().handoverCompletions(result);
        ExchangeExtension exchangeExtension = result.getExchangeExtension();
        exchangeExtension.setFromEndpoint(getEndpoint());
        exchangeExtension.setFromRouteId(getRouteId());
        return result;
    }

    @Override
    protected void onProcessingDone(Exchange original, Exchange prepared) {
        // copy result back
        ExchangeHelper.copyResults(original, prepared);
        // log exception if an exception occurred and was not handled
        if (original.getException() != null) {
            getExceptionHandler().handleException("Error processing exchange", original,
                    original.getException());
        }
    }
}
