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

import java.util.function.Function;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.karaf.core.OsgiDefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import static org.apache.karaf.camel.itests.Utils.toKebabCase;

public abstract class AbstractCamelComponent {

    protected ModelCamelContext camelContext;
    protected ServiceRegistration<CamelContext> serviceRegistration;

    public String getBaseDir() {
        return System.getProperty("project.target");
    }

    @Activate
    public void activate(ComponentContext componentContext) throws Exception {
        BundleContext bundleContext = componentContext.getBundleContext();
        camelContext = new OsgiDefaultCamelContext(bundleContext);
        serviceRegistration = bundleContext.registerService(CamelContext.class, camelContext, null);
        camelContext.start();
        camelContext.addRoutes(createRouteBuilder());
    }

    @Deactivate
    public void deactivate() {
        camelContext.stop();
        serviceRegistration.unregister();
    }

    public String getTestComponentName() {
        return toKebabCase(this.getClass().getSimpleName()).replace("-component", "-test");
    }

    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                if (producerEnabled()) {
                    configureProducer(
                            this, from("timer:producer?repeatCount=1").routeId("producer-%s".formatted(getTestComponentName()))
                    );
                }
                if (consumerEnabled()) {
                    configureConsumer(consumerRoute().apply(this));
                }
            }
        };
    }

    protected boolean producerEnabled() {
        return true;
    }

    protected boolean consumerEnabled() {
        return true;
    }

    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return null;
    }

    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        // Do nothing by default
    }

    protected void configureConsumer(RouteDefinition consumerRoute) {
        if (consumerRoute.hasCustomIdAssigned()) {
            return;
        }
        consumerRoute.routeId("consumer-%s".formatted(getTestComponentName()));
    }

    public int getNextAvailablePort() {
        return AbstractCamelKarafITest.getAvailablePort(30000, 40000);
    }
}
