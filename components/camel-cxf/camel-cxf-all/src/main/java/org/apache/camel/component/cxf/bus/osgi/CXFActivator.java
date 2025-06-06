/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.component.cxf.bus.osgi;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.bus.extension.Extension;
import org.apache.cxf.bus.extension.ExtensionRegistry;
import org.apache.cxf.common.util.CollectionUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Is called in OSGi on start and stop of the cxf bundle.
 * Manages
 * - CXFBundleListener
 * - Attaching ManagedWorkqueues to config admin service
 * - OsgiBusListener
 */
public class CXFActivator implements BundleActivator {

    private List<Extension> extensions;
    private ManagedWorkQueueList workQueues;
    private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configAdminTracker;
    private CXFExtensionBundleListener cxfBundleListener;
    private ServiceRegistration<ManagedServiceFactory> workQueueServiceRegistration;



    /** {@inheritDoc}*/
    @Override
    public void start(BundleContext context) throws Exception {
        workQueues = new ManagedWorkQueueList();
        cxfBundleListener = new CXFExtensionBundleListener(context.getBundle().getBundleId());
        context.addBundleListener(cxfBundleListener);
        cxfBundleListener.registerExistingBundles(context);

        configAdminTracker = new ServiceTracker<>(context, ConfigurationAdmin.class, null);
        configAdminTracker.open();
        workQueues.setConfigAdminTracker(configAdminTracker);
        workQueueServiceRegistration = registerManagedServiceFactory(context,
                ManagedServiceFactory.class,
                workQueues,
                ManagedWorkQueueList.FACTORY_PID);

        extensions = new ArrayList<>();
        extensions.add(createOsgiBusListenerExtension(context));
        extensions.add(createManagedWorkQueueListExtension(workQueues));
        ExtensionRegistry.addExtensions(extensions);
    }

    private <T> ServiceRegistration<T> registerManagedServiceFactory(BundleContext context,
                                                                     Class<T> serviceClass,
                                                                     T service,
                                                                     String servicePid) {
        return context.registerService(serviceClass, service,
                CollectionUtils.singletonDictionary(Constants.SERVICE_PID, servicePid));
    }

    private Extension createOsgiBusListenerExtension(BundleContext context) {
        Extension busListener = new Extension(OSGIBusListener.class);
        busListener.setArgs(new Object[] {context});
        return busListener;
    }

    private static Extension createManagedWorkQueueListExtension(final ManagedWorkQueueList workQueues) {
        return new Extension(ManagedWorkQueueList.class) {

            @Override
            public Object getLoadedObject() {
                return workQueues;
            }

            @Override
            public Extension cloneNoObject() {
                return this;
            }
        };
    }

    /** {@inheritDoc}*/
    @Override
    public void stop(BundleContext context) throws Exception {
        context.removeBundleListener(cxfBundleListener);
        cxfBundleListener.shutdown();
        workQueues.shutDown();
        workQueueServiceRegistration.unregister();
        configAdminTracker.close();
        ExtensionRegistry.removeExtensions(extensions);
    }

}
