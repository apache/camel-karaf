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

package org.apache.camel.karaf.core;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.spi.DataFormatResolver;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.LanguageResolver;
import org.apache.camel.spi.PackageScanClassResolver;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOsgiDefaultCamelContext extends DefaultCamelContext {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractOsgiDefaultCamelContext.class);

    private static final ThreadLocal<BundleContext> CURRENT_BUNDLE_CONTEXT = new ThreadLocal<>();

    private BundleContext bundleContext;

    protected AbstractOsgiDefaultCamelContext(BundleContext bundleContext) {
        super(init(bundleContext));
        this.bundleContext = bundleContext;
        CURRENT_BUNDLE_CONTEXT.remove();
    }

    /**
     * This is an ugly hack to set the bundle context before initializing the camel context.
     */
    private static boolean init(BundleContext bundleContext) {
        CURRENT_BUNDLE_CONTEXT.set(bundleContext);
        return false;
    }

    @Override
    protected ComponentResolver createComponentResolver() {
        LOG.debug("Using OsgiComponentResolver");
        return new OsgiComponentResolver(getBundleContext());
    }

    @Override
    protected LanguageResolver createLanguageResolver() {
        LOG.debug("Using OsgiLanguageResolver");
        return new OsgiLanguageResolver(getBundleContext());
    }

    @Override
    protected PackageScanClassResolver createPackageScanClassResolver() {
        LOG.debug("Using OsgiPackageScanClassResolver");
        return new OsgiPackageScanClassResolver(getBundleContext());
    }

    @Override
    protected FactoryFinderResolver createFactoryFinderResolver() {
        LOG.debug("Using OsgiFactoryFinderResolver");
        return new OsgiFactoryFinderResolver(getBundleContext());
    }

    @Override
    protected DataFormatResolver createDataFormatResolver() {
        LOG.debug("Using OsgiDataFormatResolver");
        return new OsgiDataFormatResolver(getBundleContext());
    }

    public BundleContext getBundleContext() {
        // If the bundle context is not set, then use the current bundle context
        return bundleContext == null ? CURRENT_BUNDLE_CONTEXT.get() : bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
