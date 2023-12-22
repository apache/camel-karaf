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
package org.apache.camel.blueprint;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used by {@link BlueprintCamelContext} to inform about state of Camel context. If running inside Karaf
 * and Karaf's BundleStateService is accessible, Camel context state will propagate as <em>extended
 * bundle state</em>.
 */
public class BlueprintCamelStateService {

    public static final Logger LOG = LoggerFactory.getLogger(BlueprintCamelStateService.class);

    public enum State {
        Starting,
        Active,
        Failure
    }

    private Map<String, State> states;
    private Map<String, Throwable> exceptions;

    private BundleContext bundleContext;

    private ServiceRegistration<?> registration;
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * One of four {@link State states} is set for given {@link org.osgi.framework.Bundle} and context Id.
     * One (blueprint) bundle may declare one or more Camel context.
     * @param contextId
     * @param state
     */
    public void setContextState(String contextId, State state) {
        setContextState(contextId, state, null);
    }

    /**
     * One of four {@link State states} is set for given {@link org.osgi.framework.Bundle} and context Id.
     * One (blueprint) bundle may declare one or more Camel context.
     * @param contextId
     * @param state
     * @param t
     */
    public void setContextState(String contextId, State state, Throwable t) {
        if (state == State.Failure) {
            LOG.warn("Changing Camel state for bundle {} to {}", bundleContext.getBundle().getBundleId(), state);
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Changing Camel state for bundle {} to {}", bundleContext.getBundle().getBundleId(), state);
        }

        if (state != null) {
            states.put(contextId, state);
        } else {
            states.remove(contextId);
        }
        if (t != null) {
            exceptions.put(contextId, t);
        } else {
            exceptions.remove(contextId);
        }
    }

    /**
     * Get states for all context registered for {@link Bundle}
     * @param bundle
     * @return
     */
    public Collection<State> getStates(Bundle bundle) {
        //return only states if provided bundle equals this bundle
        if (bundle == bundleContext.getBundle()) {
            return Collections.unmodifiableCollection(states.values());
        } else {
            return List.of();
        }
    }

    /**
     * Get exceptions for all camel contexts for this bundle
     *
     * @return
     */
    public Map<String, Throwable> getExceptions() {
        return Collections.unmodifiableMap(exceptions);
    }

    /**
     * Attempts to register Karaf-specific BundleStateService - if possible
     */
    public void init() {
        try {
            states = new ConcurrentHashMap<>();
            exceptions = new ConcurrentHashMap<>();

            registration = new KarafBundleStateServiceCreator().create(bundleContext, this);
        } catch (NoClassDefFoundError e) {
            LOG.info("Karaf BundleStateService not accessible. Bundle state won't reflect Camel context state");
        }
    }

    /**
     * Unregisters any OSGi service registered
     */
    public void destroy() {
        if (registration != null) {
            registration.unregister();
        }
        states.clear();
        states = null;
        exceptions.clear();
        exceptions = null;
    }

    /**
     * Static creator to decouple from optional Karaf classes.
     */
    private static class KarafBundleStateServiceCreator {
        public ServiceRegistration<?> create(BundleContext context, BlueprintCamelStateService camelStateService) {
            KarafBundleStateService karafBundleStateService = new KarafBundleStateService(camelStateService);
            return karafBundleStateService.register(context);
        }
    }

}
