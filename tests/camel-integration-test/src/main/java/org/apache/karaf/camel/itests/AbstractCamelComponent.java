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

import java.util.ArrayList;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.karaf.core.OsgiClassResolver;
import org.apache.camel.karaf.core.OsgiDefaultCamelContext;
import org.apache.camel.karaf.core.OsgiFactoryFinder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

public abstract class AbstractCamelComponent {

    protected ModelCamelContext camelContext;
    protected ServiceRegistration<CamelContext> serviceRegistration;


    @Activate
    public void activate(ComponentContext componentContext) throws Exception {
        BundleContext bundleContext = componentContext.getBundleContext();
        OsgiDefaultCamelContext osgiDefaultCamelContext = new OsgiDefaultCamelContext(bundleContext);
        OsgiClassResolver resolver =  new OsgiClassResolver(camelContext, bundleContext);
        osgiDefaultCamelContext.setClassResolver(resolver);
        osgiDefaultCamelContext.getCamelContextExtension().setName("context-test");
        osgiDefaultCamelContext.getCamelContextExtension().setBootstrapFactoryFinder(new OsgiFactoryFinder(bundleContext,resolver,"META-INF/services/org/apache/camel/"));

        camelContext = osgiDefaultCamelContext;
        serviceRegistration = bundleContext.registerService(CamelContext.class, camelContext, null);
        camelContext.start();
        camelContext.addRoutes(createRouteBuilder());
    }

    @Deactivate
    public void deactivate() throws Exception {
        camelContext.stop();
        camelContext.removeRouteDefinitions(new ArrayList<RouteDefinition>(camelContext.getRouteDefinitions()));
        serviceRegistration.unregister();
    }


    protected abstract RouteBuilder createRouteBuilder();
}
