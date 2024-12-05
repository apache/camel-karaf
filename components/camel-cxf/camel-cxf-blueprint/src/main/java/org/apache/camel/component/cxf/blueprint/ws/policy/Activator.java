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

package org.apache.camel.component.cxf.blueprint.ws.policy;

import org.apache.camel.component.cxf.blueprint.bus.BlueprintNameSpaceHandlerFactory;
import org.apache.camel.component.cxf.blueprint.bus.NamespaceHandlerRegisterer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        BlueprintNameSpaceHandlerFactory factory = new BlueprintNameSpaceHandlerFactory() {

            @Override
            public Object createNamespaceHandler() {
                return new PolicyBPHandler();
            }
        };
        NamespaceHandlerRegisterer
                .register(context, factory, "http://cxf.apache.org/policy", "http://www.w3.org/ns/ws-policy",
                        "http://www.w3.org/2006/07/ws-policy", "http://schemas.xmlsoap.org/ws/2004/09/policy",
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd",
                        "http://www.w3.org/2000/09/xmldsig#",
                        "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // nothing to do
    }

}
