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
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.TypeConverter;
import org.apache.camel.spi.Injector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.impl.converter.DefaultTypeConverter;
import org.apache.camel.spi.TypeConverterLoader;
import org.apache.camel.spi.TypeConverterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OsgiTypeConverterTest {

    @Mock
    private BundleContext bundleContext;
    @Mock
    private CamelContext camelContext;
    @Mock
    private Injector injector;
    @Mock
    private ServiceReference<TypeConverterLoader> serviceReference;
    @Mock
    private TypeConverterLoader loader;
    @Mock
    private TypeConverter typeConverter;
    @Mock
    private Bundle bundle;

    private OsgiTypeConverter osgiTypeConverter;

    @BeforeEach
    void setUp() {
        lenient().when(bundleContext.getService(serviceReference)).thenReturn(loader);
        lenient().when(serviceReference.getBundle()).thenReturn(bundle);
        lenient().when(bundle.getSymbolicName()).thenReturn("test-bundle");
        osgiTypeConverter = new OsgiTypeConverter(bundleContext, camelContext, injector);
    }

    @Test
    void addingServiceShouldLoadIntoExistingDelegate() throws Exception {
        // trigger delegate creation
        var delegate = osgiTypeConverter.getDelegate();
        assertNotNull(delegate, "delegate should be created");

        // simulate a new TypeConverterLoader service arriving
        osgiTypeConverter.addingService(serviceReference);

        // the loader should have been loaded into the existing delegate
        verify(loader).load(delegate);

        // the delegate should be the same instance (not invalidated)
        assertSame(delegate, osgiTypeConverter.getDelegate(),
            "delegate should be preserved when a new loader arrives");
    }

    @Test
    void addingServiceShouldNotFailWhenDelegateIsNull() throws Exception {
        // delegate is null initially, adding a service should not fail
        // and should not attempt to load (no delegate to load into)
        osgiTypeConverter.addingService(serviceReference);

        verify(loader, never()).load(any());

        // delegate should still be lazily created on next access
        assertNotNull(osgiTypeConverter.getDelegate());
    }

    @Test
    void newDelegateIncludesLateArrivingLoader() throws Exception {
        // simulate a loader arriving before delegate is created
        osgiTypeConverter.addingService(serviceReference);

        // when delegate is created, it should pick up the loader
        // via tracker.getServiceReferences() in createRegistry()
        var delegate = osgiTypeConverter.getDelegate();
        assertNotNull(delegate);
    }

    @Test
    void removedServiceShouldInvalidateDelegate() throws Exception {
        // trigger delegate creation
        osgiTypeConverter.getDelegate();

        // simulate service removal
        osgiTypeConverter.removedService(serviceReference, loader);

        // delegate should be rebuilt on next access
        var delegateAfter = osgiTypeConverter.getDelegate();
        assertNotNull(delegateAfter);
    }

    @Test
    void concurrentFirstAccessShouldBuildTheRegistryOnce() throws Exception {
        int threads = 16;
        AtomicInteger created = new AtomicInteger();
        CountDownLatch startLine = new CountDownLatch(1);

        OsgiTypeConverter counting = new OsgiTypeConverter(bundleContext, camelContext, injector) {
            @Override
            protected DefaultTypeConverter createRegistry() {
                created.incrementAndGet();
                return super.createRegistry();
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<DefaultTypeConverter>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    startLine.await();
                    return counting.getDelegate();
                }));
            }
            // release them all at once so they race on the null check
            startLine.countDown();

            DefaultTypeConverter first = futures.get(0).get(30, TimeUnit.SECONDS);
            assertNotNull(first);
            for (Future<DefaultTypeConverter> f : futures) {
                assertSame(first, f.get(30, TimeUnit.SECONDS),
                        "every caller must see the same registry instance");
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, created.get(),
                "the registry must be built exactly once, otherwise converters registered on a discarded"
                        + " instance are silently lost");
    }

    /** Marker source type, so the registered converter cannot collide with a core one. */
    interface Marker {
    }

    @Test
    void rebuiltRegistryReloadsTheTrackedLoadersWithoutAskingTheTracker() throws Exception {
        // arrives before the registry exists, so addingService only records it
        osgiTypeConverter.addingService(serviceReference);
        verify(loader, never()).load(any());

        DefaultTypeConverter first = osgiTypeConverter.getDelegate();

        // createRegistry replayed it from the recorded loaders; it never called back into the ServiceTracker,
        // which is what would put a framework call underneath this instance's monitor
        verify(loader).load(first);
    }

    @Test
    void programmaticConverterSurvivesARegistryRebuild() throws Exception {
        DefaultTypeConverter before = osgiTypeConverter.getDelegate();
        osgiTypeConverter.addTypeConverter(String.class, Marker.class, typeConverter);
        assertNotNull(before.lookup(String.class, Marker.class), "precondition: the converter is registered");

        // one loader going away discards the whole registry
        osgiTypeConverter.removedService(serviceReference, loader);
        DefaultTypeConverter after = osgiTypeConverter.getDelegate();

        assertNotSame(before, after, "the registry should have been rebuilt");
        assertNotNull(after.lookup(String.class, Marker.class),
                "a converter registered programmatically must be replayed onto the rebuilt registry, otherwise it"
                        + " disappears from a running context when any bundle unregisters a loader");
    }

    @Test
    void removedTypeConverterIsNotResurrectedByARebuild() throws Exception {
        osgiTypeConverter.addTypeConverter(String.class, Marker.class, typeConverter);
        osgiTypeConverter.removeTypeConverter(String.class, Marker.class);

        osgiTypeConverter.removedService(serviceReference, loader);

        assertNull(osgiTypeConverter.getDelegate().lookup(String.class, Marker.class),
                "the replay must reproduce the sequence, not just the additions");
    }

    @Test
    void addingServiceReleasesTheServiceWhenLoadingFails() throws Exception {
        osgiTypeConverter.getDelegate();
        doThrow(new RuntimeException("boom")).when(loader).load(any());

        assertThrows(RuntimeCamelException.class, () -> osgiTypeConverter.addingService(serviceReference));

        // a customizer that throws is treated as never tracked, so removedService will not run for this
        // reference and nothing else would release the use count taken by addingService
        verify(bundleContext).ungetService(serviceReference);
    }

    @Test
    void removedServiceReleasesTheService() {
        osgiTypeConverter.addingService(serviceReference);

        osgiTypeConverter.removedService(serviceReference, loader);

        verify(bundleContext).ungetService(serviceReference);
    }

    @Test
    void addingServiceMustNotHoldTheInstanceMonitor() throws Exception {
        // build first, so addingService takes the branch that calls into the loader
        osgiTypeConverter.getDelegate();

        CountDownLatch insideLoad = new CountDownLatch(1);
        CountDownLatch releaseLoad = new CountDownLatch(1);
        doAnswer(invocation -> {
            insideLoad.countDown();
            releaseLoad.await(30, TimeUnit.SECONDS);
            return null;
        }).when(loader).load(any());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> adding = pool.submit(() -> osgiTypeConverter.addingService(serviceReference));
            assertTrue(insideLoad.await(30, TimeUnit.SECONDS), "addingService should have reached loader.load");

            // the framework calls addingService while it is dispatching a service event; if it took this
            // instance's monitor, every conversion in the container would block behind an arbitrary bundle's
            // loader for as long as that loader takes
            Future<DefaultTypeConverter> reader = pool.submit(osgiTypeConverter::getDelegate);
            assertNotNull(reader.get(10, TimeUnit.SECONDS),
                    "getDelegate must not be blocked by an in-flight addingService");

            releaseLoad.countDown();
            adding.get(30, TimeUnit.SECONDS);
        } finally {
            releaseLoad.countDown();
            pool.shutdownNow();
        }
    }
}
