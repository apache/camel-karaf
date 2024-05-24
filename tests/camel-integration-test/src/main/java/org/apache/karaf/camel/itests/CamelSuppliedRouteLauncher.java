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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CamelKarafTestHint(camelContextName = CamelSuppliedRouteLauncher.CAMEL_CONTEXT_NAME)
@Component(
        name = "camel-supplied-route-launcher",
        immediate = true
)
public class CamelSuppliedRouteLauncher extends AbstractCamelRouteLauncher implements ServiceListener {

    public static final String CAMEL_CONTEXT_NAME = "supplied-route-launcher";
    private static final Logger LOG = LoggerFactory.getLogger(CamelSuppliedRouteLauncher.class);
    private final Map<String, List<RouteDefinition>> routes = new ConcurrentHashMap<>();

    @Override
    public void activate(ComponentContext componentContext) throws Exception {
        super.activate(componentContext);
        camelContext.getBundleContext().addServiceListener(this);
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // no routes to add here as they will be added by the listener
            }
        };
    }

    @Override
    public void serviceChanged(ServiceEvent serviceEvent) {
        if (serviceEvent.getType() == ServiceEvent.REGISTERED) {
            if (camelContext.getBundleContext().getService(serviceEvent.getServiceReference()) instanceof CamelRouteSupplier supplier) {
                LOG.info("CamelRouteSupplier service registered: {}", supplier.getClass().getName());
                addRoutes(supplier);
            }
        } else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING
                && camelContext.getBundleContext().getService(serviceEvent.getServiceReference()) instanceof CamelRouteSupplier supplier) {
            LOG.info("CamelRouteSupplier service unregistered: {}", supplier.getClass().getName());
            removeRoutes(supplier);
        }
    }

    private void removeRoutes(CamelRouteSupplier supplier) {
        try {
            List<RouteDefinition> routeDefinitions = routes.remove(supplier.getClass().getName());
            if (routeDefinitions == null) {
                return;
            }
            camelContext.removeRouteDefinitions(routeDefinitions);
            LOG.info("Route(s) removed from CamelRouteSupplier service: {}", supplier.getClass().getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addRoutes(CamelRouteSupplier supplier) {
        try {
            List<RouteDefinition> before = new ArrayList<>(camelContext.getRouteDefinitions());
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    supplier.configure(camelContext);
                    supplier.createRoutes(this);
                }
            });
            List<RouteDefinition> added = new ArrayList<>(camelContext.getRouteDefinitions());
            added.removeAll(before);
            if (routes.putIfAbsent(supplier.getClass().getName(), added) == null) {
                LOG.info("Route(s) created from CamelRouteSupplier service: {}", supplier.getClass().getName());
            } else {
                LOG.warn("Route(s) already created from CamelRouteSupplier service: {}", supplier.getClass().getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
