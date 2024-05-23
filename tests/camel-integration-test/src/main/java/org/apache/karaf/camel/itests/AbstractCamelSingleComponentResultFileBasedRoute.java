/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.karaf.camel.itests;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

public abstract class AbstractCamelSingleComponentResultFileBasedRoute implements CamelSingleComponentResultFileBasedRoute {

    private final CamelContext context;
    private final ProducerTemplate template;
    private final String baseDir;

    protected AbstractCamelSingleComponentResultFileBasedRoute(CamelContext context, ProducerTemplate template, String baseDir) {
        this.context = context;
        this.template = template;
        this.baseDir = baseDir;
    }

    @Override
    public String getBaseDir() {
        return baseDir;
    }

    @Override
    public CamelContext getContext() {
        return context;
    }

    @Override
    public ProducerTemplate getTemplate() {
        return template;
    }

    public void testRoutes() throws Exception {
        triggerProducerRoute();
        executeTest();
    }

    protected abstract void executeTest() throws Exception;
}
