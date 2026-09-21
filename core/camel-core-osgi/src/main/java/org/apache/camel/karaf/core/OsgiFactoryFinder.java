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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;

import org.apache.camel.impl.engine.DefaultFactoryFinder;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.util.IOHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsgiFactoryFinder extends DefaultFactoryFinder {
    private static final Logger LOG = LoggerFactory.getLogger(OsgiFactoryFinder.class);

    private final BundleContext bundleContext;

    public OsgiFactoryFinder(BundleContext bundleContext, ClassResolver classResolver, String resourcePath) {
        super(classResolver, resourcePath);
        this.bundleContext = bundleContext;
    }

    // package private rather than private: getResource is public and returns it, so private was never
    // actually restricting anything, and it keeps the type reachable from the tests in this package
    static class BundleEntry {
        URL url;
        Bundle bundle;
    }

    @Override
    public Optional<Class<?>> findClass(String key) {
        return findClassFromBundles(key, true);
    }

    // DefaultFactoryFinder implements findClass and findOptionalClass as two independent methods
    // (different lookup semantics upstream, but both ultimately resolve a class by key here), each
    // with its own default (non-OSGi) implementation. Overriding only findClass leaves
    // findOptionalClass silently falling back to the parent's plain classloader-based lookup, which
    // cannot see resources exported by other bundles - e.g. Camel's ResourceResolver SPI
    // (META-INF/services/org/apache/camel/resource-resolver/<scheme>) is looked up via
    // findOptionalClass, so a component polling a non-http resource (like camel-atom reading a
    // file: URI) would fail to find the "file" resolver bundled in camel-base-engine even though it
    // is installed, unless this method is also bridged across bundles.
    @Override
    public Optional<Class<?>> findOptionalClass(String key) {
        return findClassFromBundles(key, false);
    }

    // mandatory mirrors DefaultFactoryFinder.doNewInstance(properties, mandatory): findClass passes
    // true and throws when the descriptor is missing its "class" property, findOptionalClass passes
    // false and returns Optional.empty() instead, so a malformed descriptor doesn't poison an
    // otherwise-optional lookup for the life of the context.
    private Optional<Class<?>> findClassFromBundles(String key, boolean mandatory) {
        Class<?> answer = addToClassMap(key, () -> {
            BundleEntry entry = getResource(key);
            if (entry != null) {
                URL url = entry.url;
                InputStream in = url.openStream();
                // lets load the file
                BufferedInputStream reader = null;
                try {
                    reader = IOHelper.buffered(in);
                    Properties properties = new Properties();
                    properties.load(reader);
                    String className = properties.getProperty("class");
                    if (className == null) {
                        if (mandatory) {
                            throw new IOException("Expected property is missing: class");
                        }
                        return null;
                    }
                    return entry.bundle.loadClass(className);
                } finally {
                    IOHelper.close(reader, key, null);
                    IOHelper.close(in, key, null);
                }
            } else {
                return null;
            }
        });

        return Optional.ofNullable(answer);
    }

    // As the META-INF of the Factory could not be export,
    // we need to go through the bundles to look for it
    // NOTE, the first found factory will be return
    public BundleEntry getResource(String name) {
        BundleEntry entry = null;
        Bundle[] bundles;

        bundles = bundleContext.getBundles();

        String path = getResourcePath() + name;
        URL url;
        for (Bundle bundle : bundles) {
            // getBundles() is a snapshot, so a bundle in it can be uninstalled by the time we get here,
            // for instance by a concurrent feature:uninstall or bundle:update. getEntry then throws
            // IllegalStateException, and findClass calls us from inside addToClassMap, which caches the
            // failure in classesNotFoundExceptions and rethrows it for every later lookup of this key.
            // A bundle unrelated to the factory would poison the key for the life of the context, so
            // skip such a bundle instead of letting it fail the whole scan.
            if (bundle.getState() == Bundle.UNINSTALLED) {
                continue;
            }
            try {
                url = bundle.getEntry(path);
            } catch (IllegalStateException e) {
                // uninstalled between the state check and here
                continue;
            }
            if (url != null) {
                entry = new BundleEntry();
                entry.url = url;
                entry.bundle = bundle;
                break;
            }
        }

        if (LOG.isDebugEnabled()) {
            // which bundle wins is the container's install order, and findClass caches the answer per key,
            // so record the choice: it is the only way an operator can tell which bundle actually supplies
            // a factory when several provide the same descriptor
            if (entry == null) {
                LOG.debug("Factory descriptor {} is not provided by any installed bundle", path);
            } else {
                LOG.debug("Factory descriptor {} resolved from bundle {}/{} [{}]", path,
                        entry.bundle.getSymbolicName(), entry.bundle.getVersion(), entry.bundle.getBundleId());
            }
        }

        return entry;
    }

}
