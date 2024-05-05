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
package org.apache.camel.karaf.shell;

import org.apache.camel.CamelContext;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class CamelCommandSupport {

    private static final Logger LOG = LoggerFactory.getLogger(CamelCommandSupport.class);

    @Reference
    private BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public List<CamelContext> getCamelContexts()  {
        List<CamelContext> camelContexts = new ArrayList<>();

        try {
            ServiceReference<?>[] references = bundleContext.getServiceReferences(CamelContext.class.getName(), null);
            if (references != null) {
                for (ServiceReference<?> reference : references) {
                    if (reference != null) {
                        CamelContext camelContext = (CamelContext) bundleContext.getService(reference);
                        if (camelContext != null) {
                            camelContexts.add(camelContext);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Cannot retrieve the list of Camel contexts.", e);
        }

        // sort the list
        camelContexts.sort(Comparator.comparing(CamelContext::getName));

        return camelContexts;
    }


    public CamelContext getCamelContext(String name) throws Exception {
        for (CamelContext camelContext : getCamelContexts()) {
            if (camelContext.getName().equals(name)) {
                return camelContext;
            }
        }
        return null;
    }

}
