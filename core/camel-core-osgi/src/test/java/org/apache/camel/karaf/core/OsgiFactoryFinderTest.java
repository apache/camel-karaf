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

import java.net.URI;
import java.net.URL;

import org.apache.camel.spi.ClassResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OsgiFactoryFinderTest {

    private static final String PATH = "META-INF/services/org/apache/camel/";
    private static final String KEY = "some-factory";

    @Mock
    private BundleContext bundleContext;
    @Mock
    private ClassResolver classResolver;

    private OsgiFactoryFinder finder() {
        return new OsgiFactoryFinder(bundleContext, classResolver, PATH);
    }

    private static URL url(String spec) {
        try {
            return URI.create(spec).toURL();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Builds the bundle mocks eagerly, before the {@code when(bundleContext.getBundles())} that hands them
     * out is opened: stubbing a mock while an outer stubbing is still pending makes Mockito report
     * UnfinishedStubbing.
     * <p>
     * Stubbed leniently because the scan stops at the first provider, so a bundle placed after it is
     * legitimately never consulted.
     */
    private Bundle bundle(URL entry) {
        Bundle bundle = mock(Bundle.class);
        lenient().when(bundle.getState()).thenReturn(Bundle.ACTIVE);
        lenient().when(bundle.getEntry(PATH + KEY)).thenReturn(entry);
        return bundle;
    }

    private Bundle uninstalledBundle() {
        Bundle bundle = mock(Bundle.class);
        lenient().when(bundle.getState()).thenReturn(Bundle.UNINSTALLED);
        return bundle;
    }

    /**
     * A bundle that has been uninstalled since {@code getBundles()} snapshotted it, but is still reporting
     * a live state: {@code getEntry} is specified to throw for an uninstalled bundle.
     */
    private Bundle bundleUninstalledMidScan() {
        Bundle bundle = mock(Bundle.class);
        lenient().when(bundle.getState()).thenReturn(Bundle.ACTIVE);
        lenient().when(bundle.getEntry(PATH + KEY)).thenThrow(new IllegalStateException("bundle is uninstalled"));
        return bundle;
    }

    @Test
    public void testReturnsNullWhenNoBundleProvidesTheDescriptor() {
        Bundle a = bundle(null);
        Bundle b = bundle(null);
        when(bundleContext.getBundles()).thenReturn(new Bundle[] {a, b});

        assertNull(finder().getResource(KEY));
    }

    @Test
    public void testReturnsTheOnlyProvider() {
        URL url = url("file:///a");
        Bundle only = bundle(url);
        Bundle other = bundle(null);
        when(bundleContext.getBundles()).thenReturn(new Bundle[] {only, other});

        OsgiFactoryFinder.BundleEntry entry = finder().getResource(KEY);
        assertNotNull(entry);
        assertSame(only, entry.bundle);
        assertSame(url, entry.url);
    }

    /**
     * Selection is first-match-wins on the container's bundle install order. That is the behaviour the rest
     * of the resolution path depends on, and findClass caches it per key, so pin it against future edits.
     */
    @Test
    public void testFirstProviderWinsWhenSeveralProvideTheDescriptor() {
        URL first = url("file:///first");
        URL second = url("file:///second");
        Bundle winner = bundle(first);
        Bundle loser = bundle(second);
        when(bundleContext.getBundles()).thenReturn(new Bundle[] {winner, loser});

        OsgiFactoryFinder.BundleEntry entry = finder().getResource(KEY);
        assertNotNull(entry);
        assertSame(winner, entry.bundle, "install order decides, and the choice is cached by findClass");
        assertSame(first, entry.url);
    }

    /**
     * The scan must not ask an uninstalled bundle for an entry: it is specified to throw
     * IllegalStateException, which DefaultFactoryFinder.addToClassMap would cache in
     * classesNotFoundExceptions and rethrow for every later lookup of the same key.
     */
    @Test
    public void testSkipsUninstalledBundle() {
        Bundle uninstalled = uninstalledBundle();
        URL url = url("file:///a");
        Bundle provider = bundle(url);
        when(bundleContext.getBundles()).thenReturn(new Bundle[] {uninstalled, provider});

        OsgiFactoryFinder.BundleEntry entry = finder().getResource(KEY);
        assertNotNull(entry);
        assertSame(provider, entry.bundle);
        verify(uninstalled, never()).getEntry(anyString());
    }

    /**
     * Same race, but lost between the state check and the getEntry call: a bundle uninstalled concurrently
     * must not fail the lookup for every other bundle.
     */
    @Test
    public void testBundleUninstalledMidScanDoesNotFailTheLookup() {
        Bundle racing = bundleUninstalledMidScan();
        URL url = url("file:///a");
        Bundle provider = bundle(url);
        when(bundleContext.getBundles()).thenReturn(new Bundle[] {racing, provider});

        OsgiFactoryFinder.BundleEntry entry = finder().getResource(KEY);
        assertNotNull(entry);
        assertSame(provider, entry.bundle);
        assertSame(url, entry.url);
    }

    @Test
    public void testBundleUninstalledMidScanStillReturnsNullWhenNobodyProvides() {
        Bundle racing = bundleUninstalledMidScan();
        Bundle other = bundle(null);
        when(bundleContext.getBundles()).thenReturn(new Bundle[] {racing, other});

        assertNull(finder().getResource(KEY));
    }
}
