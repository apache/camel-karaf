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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.Servlet;
import org.apache.cxf.common.logging.LogUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

class ServletExporter implements ManagedService {
    protected static final Logger LOG = LogUtils.getL7dLogger(ServletExporter.class);
    private static final String CXF_SERVLET_PREFIX = "org.apache.cxf.servlet.";

    private final Servlet servlet;
    private final BundleContext bundleContext;
    private ServiceRegistration<Servlet> servletServiceRegistration;

    ServletExporter(Servlet servlet, BundleContext bundleContext) {
        this.servlet = servlet;
        this.bundleContext = bundleContext;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void updated(Dictionary properties) throws ConfigurationException {
        if (servletServiceRegistration != null) {
            try {
                LOG.log(Level.INFO, "Unregistering previous CXF servlet");
                servletServiceRegistration.unregister();
            } catch (IllegalStateException e) {
                LOG.log(Level.FINE, "CXF OSGi servlet was already unregistered: " + e.getMessage());
            }
            servletServiceRegistration = null;
            if (properties == null) {
                return;
            }
        }
        if (properties == null) {
            properties = new Properties();
        }

        String context = (String) getProp(properties, CXF_SERVLET_PREFIX + "context", "/cxf");
        String servletName = (String) getProp(properties, CXF_SERVLET_PREFIX + "name", "cxf-osgi-transport-servlet");

        // Build Servlet Whiteboard service properties
        Hashtable<String, Object> whiteboardProps = new Hashtable<>();
        whiteboardProps.put("osgi.http.whiteboard.servlet.name", servletName);
        whiteboardProps.put("osgi.http.whiteboard.servlet.pattern", context + "/*");
        whiteboardProps.put("osgi.http.whiteboard.servlet.async-supported",
                Boolean.valueOf(getProp(properties, CXF_SERVLET_PREFIX + "async-supported", "true").toString()));

        // Pass CXF servlet init parameters
        whiteboardProps.put("servlet.init.hide-service-list-page",
                getProp(properties, CXF_SERVLET_PREFIX + "hide-service-list-page", "false"));
        whiteboardProps.put("servlet.init.disable-address-updates",
                getProp(properties, CXF_SERVLET_PREFIX + "disable-address-updates", "true"));
        whiteboardProps.put("servlet.init.base-address",
                getProp(properties, CXF_SERVLET_PREFIX + "base-address", ""));
        whiteboardProps.put("servlet.init.service-list-path",
                getProp(properties, CXF_SERVLET_PREFIX + "service-list-path", ""));
        whiteboardProps.put("servlet.init.static-resources-list",
                getProp(properties, CXF_SERVLET_PREFIX + "static-resources-list", ""));
        whiteboardProps.put("servlet.init.redirects-list",
                getProp(properties, CXF_SERVLET_PREFIX + "redirects-list", ""));
        whiteboardProps.put("servlet.init.redirect-servlet-name",
                getProp(properties, CXF_SERVLET_PREFIX + "redirect-servlet-name", ""));
        whiteboardProps.put("servlet.init.redirect-servlet-path",
                getProp(properties, CXF_SERVLET_PREFIX + "redirect-servlet-path", ""));
        whiteboardProps.put("servlet.init.service-list-all-contexts",
                getProp(properties, CXF_SERVLET_PREFIX + "service-list-all-contexts", ""));
        whiteboardProps.put("servlet.init.service-list-page-authenticate",
                getProp(properties, CXF_SERVLET_PREFIX + "service-list-page-authenticate", "false"));
        whiteboardProps.put("servlet.init.service-list-page-authenticate-realm",
                getProp(properties, CXF_SERVLET_PREFIX + "service-list-page-authenticate-realm", "karaf"));
        whiteboardProps.put("servlet.init.use-x-forwarded-headers",
                getProp(properties, CXF_SERVLET_PREFIX + "use-x-forwarded-headers", "false"));

        try {
            LOG.log(Level.INFO, "Registering CXF servlet on " + context + " via Servlet Whiteboard");
            servletServiceRegistration = bundleContext.registerService(Servlet.class, servlet, whiteboardProps);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error registering CXF OSGi servlet " + e.getMessage(), e);
        }
    }

    void destroy() {
        if (servletServiceRegistration != null) {
            try {
                servletServiceRegistration.unregister();
            } catch (IllegalStateException e) {
                // already unregistered
            }
            servletServiceRegistration = null;
        }
    }

    @SuppressWarnings("rawtypes")
    private Object getProp(Dictionary properties, String key, Object defaultValue) {
        Object value = properties.get(key);
        return value == null ? defaultValue : value;
    }

}
