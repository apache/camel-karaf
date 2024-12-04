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

package org.apache.camel.component.cxf.blueprint.bus;

import org.apache.camel.component.cxf.blueprint.helpers.CXFAPINamespaceHandler;
import org.apache.camel.component.cxf.blueprint.helpers.CxfNamespaceHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Is called in OSGi on start and stop of the cxf bundle.
 * Manages
 * - Blueprint namespaces
 */
public class CXFActivator implements BundleActivator {

    /** {@inheritDoc}*/
    @Override
    public void start(BundleContext context) throws Exception {
        BlueprintNameSpaceHandlerFactory factory = new BlueprintNameSpaceHandlerFactory() {

            @Override
            public Object createNamespaceHandler() {
                return new CXFAPINamespaceHandler();
            }
        };
        NamespaceHandlerRegisterer.register(context, factory,
                "http://cxf.apache.org/blueprint/core",
                "http://cxf.apache.org/configuration/beans",
                "http://cxf.apache.org/configuration/parameterized-types",
                "http://cxf.apache.org/configuration/security",
                "http://schemas.xmlsoap.org/wsdl/",
                "http://www.w3.org/2005/08/addressing",
                "http://schemas.xmlsoap.org/ws/2004/08/addressing");
        factory = new BlueprintNameSpaceHandlerFactory() {

            @Override
            public Object createNamespaceHandler() {
                return new CxfNamespaceHandler();
            }
        };
        NamespaceHandlerRegisterer.register(context, factory,
                "https://camel.apache.org/schema/blueprint/cxf");

    }

    /** {@inheritDoc}*/
    @Override
    public void stop(BundleContext context) throws Exception {
        // nothing to do
    }
}
