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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.TypeConverter;
import org.apache.camel.TypeConverterExists;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.converter.DefaultTypeConverter;
import org.apache.camel.spi.BulkTypeConverters;
import org.apache.camel.spi.Injector;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.TypeConverterLoader;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.spi.TypeConvertible;
import org.apache.camel.support.SimpleTypeConverter;
import org.apache.camel.support.scan.DefaultPackageScanClassResolver;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.support.service.ServiceSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsgiTypeConverter extends ServiceSupport implements TypeConverter, TypeConverterRegistry,
        ServiceTrackerCustomizer<TypeConverterLoader, Object> {
    private static final Logger LOG = LoggerFactory.getLogger(OsgiTypeConverter.class);

    private final BundleContext bundleContext;
    private CamelContext camelContext;
    private final Injector injector;
    private final ServiceTracker<TypeConverterLoader, Object> tracker;
    /**
     * The loaders the tracker has handed us, kept here rather than read back from the tracker: resolving them
     * through the tracker inside {@link #createRegistry()} would mean calling into the ServiceTracker and the
     * framework while holding this instance's monitor.
     */
    private final Map<ServiceReference<TypeConverterLoader>, TypeConverterLoader> trackedLoaders
            = new ConcurrentHashMap<>();
    /**
     * Registrations made through this facade rather than by a {@link TypeConverterLoader}, so a rebuilt registry can
     * be brought back to the same state. Discarding the delegate would otherwise drop them with no way to get them
     * back.
     * <p/>
     * Keyed rather than a plain list, and insertion ordered so replay keeps the original order. The key is what the
     * registration is <em>about</em> - a {@link TypeConvertible} for a converter, the contributed instance for the
     * bulk and fallback forms, a sentinel for each setter - so re-registering replaces instead of appending, and
     * removing deletes the entry instead of appending an inverse. Both matter in OSGi: each registration strongly
     * references the converter it captured, and through it the classloader of the bundle that contributed it, so a
     * collection that only ever grows pins bundles that have long since been uninstalled.
     * <p/>
     * Guarded by this instance's monitor, the same one {@link #getDelegate()} takes.
     */
    private final Map<Object, Consumer<TypeConverterRegistry>> programmaticRegistrations = new LinkedHashMap<>();

    private static final Object INJECTOR_KEY = new Object();
    private static final Object TYPE_CONVERTER_EXISTS_KEY = new Object();
    private static final Object TYPE_CONVERTER_EXISTS_LOGGING_LEVEL_KEY = new Object();
    private volatile DefaultTypeConverter delegate;
    private volatile boolean trackerOpened;

    public OsgiTypeConverter(BundleContext bundleContext, CamelContext camelContext, Injector injector) {
        this.bundleContext = bundleContext;
        this.camelContext = camelContext;
        this.injector = injector;
        this.tracker = new ServiceTracker<>(bundleContext, TypeConverterLoader.class.getName(), this);
    }

    private synchronized void ensureTrackerOpen() {
        if (!trackerOpened) {
            tracker.open();
            trackerOpened = true;
        }
    }

    // deliberately not synchronized: the tracker calls this from the framework's service event dispatch, and
    // taking this instance's monitor here would put our lock on the far side of the framework's, which is the
    // ordering that makes a lock inversion possible
    @Override
    public Object addingService(ServiceReference<TypeConverterLoader> serviceReference) {
        LOG.trace("AddingService: {}, Bundle: {}", serviceReference, serviceReference.getBundle());
        TypeConverterLoader loader = bundleContext.getService(serviceReference);
        if (loader != null) {
            trackedLoaders.put(serviceReference, loader);
            try {
                LOG.debug("loading type converter from bundle: {}", serviceReference.getBundle().getSymbolicName());
                DefaultTypeConverter current = delegate;
                if (current != null) {
                    // load the converter directly into the existing delegate to preserve
                    // any converters that were added programmatically (e.g. via Blueprint beans
                    // implementing TypeConverters)
                    loader.load(current);
                }
            } catch (Throwable t) {
                // the tracker treats a customizer that throws as "never tracked", so it will not call
                // removedService for this reference and nothing else will release the use count taken above
                trackedLoaders.remove(serviceReference);
                ungetQuietly(serviceReference);
                throw new RuntimeCamelException("Error loading type converters from service: " + serviceReference + " due: " + t.getMessage(), t);
            }
        }

        return loader;
    }

    @Override
    public void modifiedService(ServiceReference<TypeConverterLoader> serviceReference, Object o) {
    }

    // not synchronized, for the same reason as addingService
    @Override
    public void removedService(ServiceReference<TypeConverterLoader> serviceReference, Object o) {
        LOG.trace("RemovedService: {}, Bundle: {}", serviceReference, serviceReference.getBundle());
        trackedLoaders.remove(serviceReference);
        // we took the service in addingService, so releasing it is ours to do
        ungetQuietly(serviceReference);
        if (this.delegate != null && !isStopping() && !isStopped()) {
            // worth saying out loud: one loader going away discards the whole registry, and the rebuild is a
            // full reload of the core converters plus every remaining loader, not an incremental removal
            LOG.warn("TypeConverterLoader from bundle {} was unregistered, discarding the type converter registry;"
                     + " it is rebuilt on next use from the remaining loaders, and the converters registered"
                     + " programmatically on this context are replayed onto the rebuilt registry.",
                    serviceReference.getBundle() != null ? serviceReference.getBundle().getSymbolicName() : serviceReference);
        }
        try {
            ServiceHelper.stopService(this.delegate);
        } catch (Exception e) {
            // ignore
            LOG.debug("Error stopping service due: " + e.getMessage() + ". This exception will be ignored.", e);
        }
        // It can force camel to reload the type converter again
        this.delegate = null;
    }

    private void ungetQuietly(ServiceReference<TypeConverterLoader> serviceReference) {
        try {
            bundleContext.ungetService(serviceReference);
        } catch (Exception e) {
            // the bundle or the framework may already be gone; releasing the use count is best effort
            LOG.debug("Error ungetting service {} due: {}. This exception will be ignored.", serviceReference,
                    e.getMessage(), e);
        }
    }

    @Override
    protected void doStart() throws Exception {
        ensureTrackerOpen();
    }

    @Override
    protected void doStop() throws Exception {
        this.tracker.close();
        this.trackerOpened = false;
        // close() calls removedService for everything still tracked, this only makes the end state explicit
        this.trackedLoaders.clear();
        synchronized (this) {
            // ServiceSupport allows stop then start again, and this is a context scoped service, so without this a
            // restart would replay the previous lifecycle's registrations - including converters belonging to
            // bundles that are gone by the time the context comes back up
            this.programmaticRegistrations.clear();
        }
        ServiceHelper.stopService(this.delegate);
        this.delegate = null;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public boolean allowNull() {
        return getDelegate().allowNull();
    }

    @Override
    public <T> T convertTo(Class<T> type, Object value) {
        return getDelegate().convertTo(type, value);
    }

    @Override
    public <T> T convertTo(Class<T> type, Exchange exchange, Object value) {
        return getDelegate().convertTo(type, exchange, value);
    }

    @Override
    public <T> T mandatoryConvertTo(Class<T> type, Object value) throws NoTypeConversionAvailableException {
        return getDelegate().mandatoryConvertTo(type, value);
    }

    @Override
    public <T> T mandatoryConvertTo(Class<T> type, Exchange exchange, Object value) throws NoTypeConversionAvailableException {
        return getDelegate().mandatoryConvertTo(type, exchange, value);
    }

    @Override
    public <T> T tryConvertTo(Class<T> type, Exchange exchange, Object value) {
        return getDelegate().tryConvertTo(type, exchange, value);
    }

    @Override
    public <T> T tryConvertTo(Class<T> type, Object value) {
        return getDelegate().tryConvertTo(type, value);
    }


    @Override
    public void addTypeConverter(Class<?> toType, Class<?> fromType, TypeConverter typeConverter) {
        register(new TypeConvertible<>(fromType, toType),
                registry -> registry.addTypeConverter(toType, fromType, typeConverter));
    }

    @Override
    public void addTypeConverters(Object typeConverters) {
        register(typeConverters, registry -> registry.addTypeConverters(typeConverters));
    }

    @Override
    public void addBulkTypeConverters(BulkTypeConverters bulkTypeConverters) {
        register(bulkTypeConverters, registry -> registry.addBulkTypeConverters(bulkTypeConverters));
    }

    @Override
    public synchronized boolean removeTypeConverter(Class<?> toType, Class<?> fromType) {
        boolean removed = getDelegate().removeTypeConverter(toType, fromType);
        // delete the matching registration rather than recording an inverse. An inverse would reproduce the same
        // end state on replay, but it would also keep the removed converter - and its bundle's classloader -
        // strongly reachable for the life of the context, and cost a no-op call on every future rebuild
        programmaticRegistrations.remove(new TypeConvertible<>(fromType, toType));
        return removed;
    }

    @Override
    public void addFallbackTypeConverter(TypeConverter typeConverter, boolean canPromote) {
        register(typeConverter, registry -> registry.addFallbackTypeConverter(typeConverter, canPromote));
    }

    @Override
    public TypeConverter lookup(Class<?> toType, Class<?> fromType) {
        return getDelegate().lookup(toType, fromType);
    }

    @Override
    public void setInjector(Injector injector) {
        register(INJECTOR_KEY, registry -> registry.setInjector(injector));
    }

    @Override
    public Injector getInjector() {
        return getDelegate().getInjector();
    }

    @Override
    public Statistics getStatistics() {
        return getDelegate().getStatistics();
    }

    @Override
    public int size() {
        return getDelegate().size();
    }

    @Override
    public LoggingLevel getTypeConverterExistsLoggingLevel() {
        return getDelegate().getTypeConverterExistsLoggingLevel();
    }

    @Override
    public void setTypeConverterExistsLoggingLevel(LoggingLevel loggingLevel) {
        register(TYPE_CONVERTER_EXISTS_LOGGING_LEVEL_KEY,
                registry -> registry.setTypeConverterExistsLoggingLevel(loggingLevel));
    }

    @Override
    public TypeConverterExists getTypeConverterExists() {
        return getDelegate().getTypeConverterExists();
    }

    @Override
    public void setTypeConverterExists(TypeConverterExists typeConverterExists) {
        register(TYPE_CONVERTER_EXISTS_KEY, registry -> registry.setTypeConverterExists(typeConverterExists));
    }

    // fully synchronized rather than double checked: the delegate is not immutable after publication -
    // removedService stops and replaces it - so a lock free read of the field buys a race for no real gain,
    // conversion work dwarfing an uncontended monitor either way
    public synchronized DefaultTypeConverter getDelegate() {
        if (delegate == null) {
            // ensure the tracker is open so we can discover TypeConverterLoader services
            // before creating the registry - this is important because getDelegate() may be
            // called during doInit() (e.g. when to() eagerly creates endpoints) which happens
            // before doStart() where the tracker is normally opened
            ensureTrackerOpen();
            delegate = createRegistry();
        }
        return delegate;
    }

    protected DefaultTypeConverter createRegistry() {
        // base the osgi type converter on the default type converter
        DefaultTypeConverter answer = new OsgiDefaultTypeConverter(new DefaultPackageScanClassResolver() {
            @Override
            public Set<ClassLoader> getClassLoaders() {
                // we only need classloaders for loading core TypeConverterLoaders
                return new HashSet<>(Arrays.asList(
                        DefaultTypeConverter.class.getClassLoader(),
                        DefaultCamelContext.class.getClassLoader()));
            }
        }, injector, false, camelContext.isTypeConverterStatisticsEnabled());

        // inject CamelContext
        answer.setCamelContext(camelContext);


        try {
            // init before loading core converters
            answer.init();
            // only load the core type converters, as OSGi activator will keep track on bundles
            // being installed/uninstalled and load type converters as part of that process
            answer.loadCoreAndFastTypeConverters();
        } catch (Exception e) {
            throw new RuntimeCamelException("Error loading CoreTypeConverter due: " + e.getMessage(), e);
        }

        // Load the type converters the tracker has been tracking. These come from our own map rather than from
        // tracker.getServiceReferences()/getService(): this runs while holding this instance's monitor, and
        // calling back into the tracker from here is what would establish a lock ordering against the framework.
        List<ServiceReference<TypeConverterLoader>> servicesList = new ArrayList<>(trackedLoaders.keySet());
        // Just make sure we install the high ranking fallback converter at last
        Collections.sort(servicesList);
        for (ServiceReference<TypeConverterLoader> sr : servicesList) {
            TypeConverterLoader loader = trackedLoaders.get(sr);
            if (loader == null) {
                // unregistered between the snapshot and here
                continue;
            }
            try {
                LOG.debug("loading type converter from bundle: {}", sr.getBundle().getSymbolicName());
                loader.load(answer);
            } catch (Throwable t) {
                throw new RuntimeCamelException("Error loading type converters from service: " + sr + " due: " + t.getMessage(), t);
            }
        }

        replayProgrammaticRegistrations(answer);

        LOG.trace("Created TypeConverter: {}", answer);
        return answer;
    }

    /**
     * Re-applies everything that was registered through this facade rather than by a
     * {@link TypeConverterLoader}, in the order it was originally applied.
     */
    private void replayProgrammaticRegistrations(DefaultTypeConverter registry) {
        if (programmaticRegistrations.isEmpty()) {
            return;
        }
        LOG.debug("Replaying {} programmatic registration(s) onto the rebuilt type converter registry",
                programmaticRegistrations.size());
        for (Consumer<TypeConverterRegistry> registration : programmaticRegistrations.values()) {
            registration.accept(registry);
        }
    }

    /**
     * Applies a registration to the current delegate and remembers it under {@code key}, so that discarding the
     * delegate does not discard the registration with it.
     * <p/>
     * Synchronized, and on the same monitor {@link #getDelegate()} takes: applying and recording have to be one
     * step. A rebuild landing between them would replay a list this registration is not in yet and then hand back
     * a registry it was never applied to, so the converter would be recorded but not actually present - the same
     * loss this class is meant to prevent, one level down. The monitor is reentrant, so the nested
     * {@link #getDelegate()} is free.
     */
    /**
     * How many programmatic registrations are currently retained for replay. Package private for the tests, which
     * pin the invariant that this does not grow without bound - each entry retains the converter it captured, and
     * with it the classloader of the contributing bundle.
     */
    synchronized int programmaticRegistrationCount() {
        return programmaticRegistrations.size();
    }

    private synchronized void register(Object key, Consumer<TypeConverterRegistry> registration) {
        // apply first: a registration the delegate rejects is not one worth replaying. getDelegate() may build the
        // registry here, replaying the map as it stands - this entry goes in after, so it cannot be applied twice
        registration.accept(getDelegate());
        programmaticRegistrations.put(key, registration);
    }

    private class OsgiDefaultTypeConverter extends DefaultTypeConverter {

        public OsgiDefaultTypeConverter(PackageScanClassResolver resolver, Injector injector, boolean loadTypeConverters,
                                        boolean statisticsEnabled) {
            super(resolver, injector, loadTypeConverters, statisticsEnabled);
        }

        @Override
        public void addTypeConverter(Class<?> toType, Class<?> fromType, TypeConverter typeConverter) {
            // favour keeping the converter that was loaded via TypeConverterLoader META-INF file
            // as OSGi loads these first and then gets triggered again later when there is both a META-INF/TypeConverter and META-INF/TypeConverterLoaded file
            // for the same set of type converters and we get duplicates (so this is a way of filtering out duplicates)
            TypeConverter converter = getTypeConverter(toType, fromType);
            if (converter != null && converter != typeConverter) {
                // the converter is already there which we want to keep (optimized via SimpleTypeConverter)
                if (converter instanceof SimpleTypeConverter) {
                    // okay keep this one
                    return;
                }
            }
            super.addTypeConverter(toType, fromType, typeConverter);
        }
    }

    @Override
    public Map<Class<?>, TypeConverter> lookup(Class<?> toType) {
        return getDelegate().lookup(toType);
    }

    @Override
    public void addConverter(TypeConvertible<?, ?> typeConvertible, TypeConverter typeConverter) {
        register(typeConvertible, registry -> registry.addConverter(typeConvertible, typeConverter));
    }

}