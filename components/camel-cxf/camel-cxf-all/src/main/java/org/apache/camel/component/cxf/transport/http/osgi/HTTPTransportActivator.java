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

package org.apache.camel.component.cxf.transport.http.osgi;

import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.DestinationRegistryImpl;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class HTTPTransportActivator implements BundleActivator {
    private static final String DISABLE_DEFAULT_HTTP_TRANSPORT = "org.apache.cxf.osgi.http.transport.disable";
    private ServiceTracker<HttpService, ?> httpServiceTracker;

    @Override
    public void start(final BundleContext context) throws Exception {

        ConfigAdminHttpConduitConfigurer conduitConfigurer = new ConfigAdminHttpConduitConfigurer();

        registerService(context, ManagedServiceFactory.class, conduitConfigurer,
                ConfigAdminHttpConduitConfigurer.FACTORY_PID);
        registerService(context, HTTPConduitConfigurer.class, conduitConfigurer,
                "org.apache.cxf.http.conduit-configurer");

        if (PropertyUtils.isTrue(context.getProperty(DISABLE_DEFAULT_HTTP_TRANSPORT))) {
            //TODO: Review if it also makes sense to support "http.transport.disable"
            //      directly in the CXF_CONFIG_SCOPE properties file
            return;
        }

        DestinationRegistry destinationRegistry = new DestinationRegistryImpl();
        HTTPTransportFactory transportFactory = new HTTPTransportFactory(destinationRegistry);

        // HttpService is no longer available in OSGI 8
//        HttpServiceTrackerCust customizer = new HttpServiceTrackerCust(destinationRegistry, context);
//        httpServiceTracker = new ServiceTracker<>(context, HttpService.class, customizer);
//        httpServiceTracker.open();

        context.registerService(DestinationRegistry.class.getName(), destinationRegistry, null);
        context.registerService(HTTPTransportFactory.class.getName(), transportFactory, null);
    }

    private <T> ServiceRegistration<T> registerService(BundleContext context, Class<T> serviceInterface,
                                                       T serviceObject, String servicePid) {
        return context.registerService(serviceInterface, serviceObject,
                CollectionUtils.singletonDictionary(Constants.SERVICE_PID, servicePid));
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (httpServiceTracker != null) {
            httpServiceTracker.close();
        }
    }
}
