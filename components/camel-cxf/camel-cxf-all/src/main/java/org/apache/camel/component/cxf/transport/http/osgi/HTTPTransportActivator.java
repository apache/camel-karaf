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

import jakarta.servlet.Servlet;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.DestinationRegistryImpl;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

public class HTTPTransportActivator implements BundleActivator {
    private static final String CXF_CONFIG_PID = "org.apache.cxf.osgi";
    private static final String DISABLE_DEFAULT_HTTP_TRANSPORT = "org.apache.cxf.osgi.http.transport.disable";

    private ServiceRegistration<ManagedService> servletPublisherReg;
    private ServletExporter servletExporter;

    @Override
    public void start(final BundleContext context) throws Exception {

        ConfigAdminHttpConduitConfigurer conduitConfigurer = new ConfigAdminHttpConduitConfigurer();

        registerService(context, ManagedServiceFactory.class, conduitConfigurer,
                ConfigAdminHttpConduitConfigurer.FACTORY_PID);
        registerService(context, HTTPConduitConfigurer.class, conduitConfigurer,
                "org.apache.cxf.http.conduit-configurer");

        if (PropertyUtils.isTrue(context.getProperty(DISABLE_DEFAULT_HTTP_TRANSPORT))) {
            return;
        }

        DestinationRegistry destinationRegistry = new DestinationRegistryImpl();
        HTTPTransportFactory transportFactory = new HTTPTransportFactory(destinationRegistry);

        // Register the CXF servlet using OSGi Servlet Whiteboard
        Servlet servlet = new CXFNonSpringServlet(destinationRegistry, false);
        servletExporter = new ServletExporter(servlet, context);
        servletPublisherReg = context.registerService(ManagedService.class,
                servletExporter,
                CollectionUtils.singletonDictionary(Constants.SERVICE_PID, CXF_CONFIG_PID));

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
        if (servletPublisherReg != null) {
            servletPublisherReg.unregister();
        }
        if (servletExporter != null) {
            servletExporter.destroy();
        }
    }
}
