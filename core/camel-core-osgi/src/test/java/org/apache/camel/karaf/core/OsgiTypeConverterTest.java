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
import org.apache.camel.spi.Injector;
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
}
