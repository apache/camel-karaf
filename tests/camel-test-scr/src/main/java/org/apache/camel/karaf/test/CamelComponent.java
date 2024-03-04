/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.camel.karaf.test;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.karaf.core.OsgiClassResolver;
import org.apache.camel.karaf.core.OsgiDataFormatResolver;
import org.apache.camel.karaf.core.OsgiDefaultCamelContext;
import org.apache.camel.karaf.core.OsgiLanguageResolver;
import org.apache.camel.karaf.core.OsgiFactoryFinder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spi.DataFormatResolver;
import org.apache.camel.spi.LanguageResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.ArrayList;

@Component(
        name = "camel-karaf-test",
        immediate = true
)
public class CamelComponent {

    private ModelCamelContext camelContext;
    private ServiceRegistration<CamelContext> serviceRegistration;

    @Activate
    public void activate(ComponentContext componentContext) throws Exception {
        BundleContext bundleContext = componentContext.getBundleContext();
        OsgiDefaultCamelContext osgiDefaultCamelContext = new OsgiDefaultCamelContext(bundleContext);
        OsgiClassResolver resolver =  new OsgiClassResolver(camelContext, bundleContext);
        osgiDefaultCamelContext.setClassResolver(resolver);
        osgiDefaultCamelContext.getCamelContextExtension().addContextPlugin(DataFormatResolver.class, new OsgiDataFormatResolver(bundleContext));
        osgiDefaultCamelContext.getCamelContextExtension().addContextPlugin(LanguageResolver.class, new OsgiLanguageResolver(bundleContext));
        osgiDefaultCamelContext.getCamelContextExtension().setName("context-test");
        osgiDefaultCamelContext.getCamelContextExtension().setBootstrapFactoryFinder(new OsgiFactoryFinder(bundleContext,resolver,"META-INF/services/org/apache/camel/"));
        camelContext = osgiDefaultCamelContext;
        serviceRegistration = bundleContext.registerService(CamelContext.class, camelContext, null);
        camelContext.start();
        camelContext.addRoutes(new RouteBuilder() {
                                   @Override
                                   public void configure() throws Exception {
                                       from("timer:fire?period=5000")
                                               .setBody(constant("Hello World"))
                                               .log("[TEST] Body: ${body}")
                                               .to("direct:inner")
                                               .setId("route-test");

                                       from("direct:inner")
                                               .log("[INNER TEST] Route consuming from direct endpoint received: ${body}")
                                               .to("mock:test")
                                               .setId("route-inner-test");
                                   }
                               }
        );
    }

    @Deactivate
    public void deactivate() throws Exception {
        camelContext.stop();
        camelContext.removeRouteDefinitions(new ArrayList<>(camelContext.getRouteDefinitions()));
        serviceRegistration.unregister();
    }

}
