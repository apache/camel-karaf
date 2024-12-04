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

package org.apache.camel.component.cxf.osgi;

import java.util.List;

import org.apache.camel.component.cxf.bus.osgi.CXFActivator;
import org.apache.camel.component.cxf.transport.http.osgi.HTTPTransportActivator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class MainActivator implements BundleActivator {

    private final List<BundleActivator> activators = List.of(
            new CXFActivator(),
            new org.apache.camel.component.cxf.ext.logging.osgi.Activator(),
            new HTTPTransportActivator());

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        for (BundleActivator activator : activators) {
            activator.start(bundleContext);
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        for (int i = activators.size() - 1; i >= 0; i--) {
            activators.get(i).stop(bundleContext);
        }
    }
}
