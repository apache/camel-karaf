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

package org.apache.camel.component.cxf.transport.http_jetty.osgi;

import java.io.IOException;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HttpDestinationFactory;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;

@NoJSR250Annotations()
public class HTTPJettyDestinationFactory implements HttpDestinationFactory {

    @Override
    public AbstractHTTPDestination createDestination(EndpointInfo endpointInfo, Bus bus, DestinationRegistry registry)
            throws IOException {
        if (bus.getExtension(ClassLoader.class) == null) {
            bus.setExtension(HTTPJettyDestinationFactory.class.getClassLoader(), ClassLoader.class);
        }
        return new JettyHTTPDestination(bus, registry, endpointInfo, bus.getExtension(JettyHTTPServerEngineFactory.class));
    }
}
