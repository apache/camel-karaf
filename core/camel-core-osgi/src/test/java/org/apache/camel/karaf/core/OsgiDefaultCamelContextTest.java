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

import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.engine.DefaultComponentResolver;
import org.apache.camel.impl.engine.DefaultLanguageResolver;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.spi.LanguageResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OsgiDefaultCamelContextTest {

    private OsgiDefaultCamelContext context;

    private BundleContext createMockBundleContext() {
        BundleContext bundleContext = mock(BundleContext.class);
        Bundle bundle = mock(Bundle.class);
        when(bundleContext.getBundle()).thenReturn(bundle);
        when(bundle.getSymbolicName()).thenReturn("test-bundle");

        // OsgiFactoryFinder iterates over all bundles looking for META-INF resources.
        // Return a bundle whose getEntry delegates to the classloader so that
        // Camel's default factory-finder resources on the classpath are found.
        Bundle resourceBundle = mock(Bundle.class);
        when(resourceBundle.getEntry(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            return OsgiDefaultCamelContextTest.class.getClassLoader().getResource(path);
        });
        try {
            when(resourceBundle.loadClass(anyString())).thenAnswer(invocation -> {
                String className = invocation.getArgument(0);
                return Class.forName(className);
            });
        } catch (ClassNotFoundException e) {
            // won't happen - this is stubbing
        }

        when(bundleContext.getBundles()).thenReturn(new Bundle[]{resourceBundle});
        return bundleContext;
    }

    /**
     * Replace OSGi-based resolvers with classpath-based defaults so the context
     * can resolve components and languages without a real OSGi service registry.
     */
    private void useClasspathResolvers(OsgiDefaultCamelContext ctx) {
        ctx.getCamelContextExtension().addContextPlugin(ComponentResolver.class, new DefaultComponentResolver());
        ctx.getCamelContextExtension().addContextPlugin(LanguageResolver.class, new DefaultLanguageResolver());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (context != null) {
            if (context.isStarted()) {
                context.stop();
            }
            context = null;
        }
    }

    @Test
    void contextShouldBeInBuiltStateAfterConstruction() {
        BundleContext bundleContext = createMockBundleContext();
        context = new OsgiDefaultCamelContext(bundleContext);

        assertTrue(context.isBuild(), "Context should be in Built state after construction");
        assertFalse(context.isNew(), "Context should no longer be in New state after construction");
    }

    @Test
    void contextShouldNotBeInInitializedStateAfterConstruction() {
        BundleContext bundleContext = createMockBundleContext();
        context = new OsgiDefaultCamelContext(bundleContext);

        // build() transitions: NEW -> BUILT
        // init() transitions: NEW -> INITIALIZED (skipping BUILT)
        // After build(), isInit() should be false — the context has been built but not yet initialized.
        // This is the key behavioral difference: build() is the correct lifecycle step here,
        // allowing the caller to add routes before init/start fully wires the context.
        assertFalse(context.isInit(), "Context should not be initialized yet, only built");
    }

    @Test
    void contextShouldStartSuccessfully() throws Exception {
        BundleContext bundleContext = createMockBundleContext();
        context = new OsgiDefaultCamelContext(bundleContext);
        useClasspathResolvers(context);

        context.start();

        assertTrue(context.isStarted(), "Context should be started");
        assertEquals(ServiceStatus.Started, context.getStatus());
    }

    @Test
    void contextShouldAcceptRoutesAfterConstruction() throws Exception {
        BundleContext bundleContext = createMockBundleContext();
        context = new OsgiDefaultCamelContext(bundleContext);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start")
                        .to("log:test")
                        .routeId("test-route");
            }
        });

        assertFalse(context.getRouteDefinitions().isEmpty(), "Route definitions should be added");
        assertEquals("test-route", context.getRouteDefinitions().get(0).getRouteId());
    }

    @Test
    void routesShouldBeStartedWhenContextStarts() throws Exception {
        BundleContext bundleContext = createMockBundleContext();
        context = new OsgiDefaultCamelContext(bundleContext);
        useClasspathResolvers(context);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start")
                        .to("log:test")
                        .routeId("test-route");
            }
        });

        context.start();

        assertNotNull(context.getRoute("test-route"), "Route should exist after start");
        assertEquals(1, context.getRoutes().size(), "There should be one running route");
    }

    @Test
    void multipleRoutesShouldBeAddedAndStarted() throws Exception {
        BundleContext bundleContext = createMockBundleContext();
        context = new OsgiDefaultCamelContext(bundleContext);
        useClasspathResolvers(context);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start")
                        .to("log:test")
                        .routeId("route-1");

                from("direct:other")
                        .to("log:other")
                        .routeId("route-2");
            }
        });

        context.start();

        assertEquals(2, context.getRoutes().size(), "Both routes should be running");
        assertNotNull(context.getRoute("route-1"));
        assertNotNull(context.getRoute("route-2"));
    }

    @Test
    void bundleContextShouldBeAccessible() {
        BundleContext bundleContext = createMockBundleContext();
        context = new OsgiDefaultCamelContext(bundleContext);

        assertSame(bundleContext, context.getBundleContext());
    }

    @Test
    void contextShouldStopCleanly() throws Exception {
        BundleContext bundleContext = createMockBundleContext();
        context = new OsgiDefaultCamelContext(bundleContext);
        useClasspathResolvers(context);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start")
                        .to("log:test")
                        .routeId("test-route");
            }
        });

        context.start();
        assertTrue(context.isStarted());

        context.stop();
        assertTrue(context.isStopped(), "Context should be stopped");
    }
}
