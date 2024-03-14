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

import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.component.seda.BlockingQueueFactory;
import org.apache.camel.component.seda.QueueReference;
import org.apache.camel.component.seda.SedaComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The VM component is for asynchronous SEDA exchanges on a {@link java.util.concurrent.BlockingQueue} within the
 * classloader tree containing the camel-core.jar.
 *
 * i.e. to handle communicating across CamelContext instances, potentially in different Karaf bundles.
 */
@org.apache.camel.spi.annotations.Component("vm")
public class VmComponent extends SedaComponent {

    protected static final Map<String, QueueReference> QUEUES = new HashMap<>();
    protected static final Map<String, VmEndpoint> ENDPOINTS = new HashMap<>();
    private static final AtomicInteger START_COUNTER = new AtomicInteger();

    public VmComponent() {}

    @Override
    public Map<String, QueueReference> getQueues() {
        return QUEUES;
    }

    @Override
    public QueueReference getQueueReference(String key) {
        return QUEUES.get(key);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        START_COUNTER.incrementAndGet();
    }

    @Override
    protected void doStop() throws Exception {
        if (START_COUNTER.decrementAndGet() <= 0) {
            // clear queues when no more vm components in use
            getQueues().clear();
            // also clear endpoints
            ENDPOINTS.clear();
        }
    }

    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        if (ENDPOINTS.containsKey(uri)) {
            return ENDPOINTS.get(uri);
        }

        VmEndpoint answer = (VmEndpoint) super.createEndpoint(uri, remaining, parameters);

        ENDPOINTS.put(uri, answer);

        return answer;
    }

    protected VmEndpoint createEndpoint(String endpointUri, Component component, BlockingQueueFactory<Exchange> queueFactory, int concurrentConsumers) {
        return new VmEndpoint(endpointUri, component, queueFactory, concurrentConsumers);
    }

    protected VmEndpoint createEndpoint(String endpointUri, Component component, BlockingQueue<Exchange> queue, int concurrentConsumers) {
        return new VmEndpoint(endpointUri, component, queue, concurrentConsumers);
    }

}
