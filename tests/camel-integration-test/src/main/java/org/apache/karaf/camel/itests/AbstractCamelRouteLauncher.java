/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.camel.itests;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.karaf.core.OsgiCamelContextPublisher;
import org.apache.camel.karaf.core.OsgiDefaultCamelContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

/**
 * The parent class of the Camel Karaf integration test for a specific Camel component/wrapper/feature.
 */
public abstract class AbstractCamelRouteLauncher {

    protected OsgiDefaultCamelContext camelContext;
    protected ServiceRegistration<?> serviceRegistration;

    @Activate
    public void activate(ComponentContext componentContext) throws Exception {
        BundleContext bundleContext = componentContext.getBundleContext();
        camelContext = new OsgiDefaultCamelContext(bundleContext);
        String contextName = Utils.getCamelContextName(getClass());
        if (contextName == null) {
            throw new IllegalStateException("Camel context name not set for " + getClass().getName() + " using @CamelKarafTestHint annotation");
        }
        camelContext.getCamelContextExtension().setName(contextName);
        serviceRegistration = new OsgiCamelContextPublisher(bundleContext).registerCamelContext(camelContext);
        if (serviceRegistration == null) {
            throw new IllegalStateException("Camel context registration failed for " + contextName + " most likely because the context name is already in use");
        }
        camelContext.start();
        camelContext.addRoutes(createRouteBuilder());
    }

    @Deactivate
    public void deactivate() {
        if (camelContext == null) {
            return;
        }
        camelContext.stop();
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

    /**
     * Create the RouteBuilder that will be used to create the Camel routes to test.
     */
    protected abstract RouteBuilder createRouteBuilder();
}
