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
package org.apache.camel.karaf.core;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.karaf.core.utils.BundleContextUtils;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.ArrayList;

/**
 * This abstract class provide a CamelContext (registered as an OSGi service) that can be directly used to register Camel routes.
 */
public abstract class KarafCamelContextProvider {

    private ModelCamelContext camelContext;
    private ServiceRegistration<CamelContext> serviceRegistration;

    public void start() throws Exception {
        start(null);
    }

    public void start(String name) throws Exception {
        BundleContext bundleContext = BundleContextUtils.getBundleContext(this.getClass());
        OsgiDefaultCamelContext osgiDefaultCamelContext = new OsgiDefaultCamelContext(bundleContext);
        osgiDefaultCamelContext.setClassResolver(new OsgiClassResolver(camelContext, bundleContext));
        if (name != null) {
            osgiDefaultCamelContext.getCamelContextExtension().setName(name);
        }
        camelContext = osgiDefaultCamelContext;
        serviceRegistration = bundleContext.registerService(CamelContext.class, camelContext, null);
        camelContext.start();
        camelContext.addRoutes(getRouteBuilder());
    }

    public void stop() throws Exception {
        camelContext.stop();
        camelContext.removeRouteDefinitions(new ArrayList<RouteDefinition>(camelContext.getRouteDefinitions()));
        serviceRegistration.unregister();
    }

    public abstract RoutesBuilder getRouteBuilder();

}
