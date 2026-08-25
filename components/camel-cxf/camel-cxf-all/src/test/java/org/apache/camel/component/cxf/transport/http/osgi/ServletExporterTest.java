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
package org.apache.camel.component.cxf.transport.http.osgi;

import java.util.Dictionary;
import java.util.Hashtable;

import jakarta.servlet.Servlet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ServletExporterTest {

    @Mock
    private Servlet servlet;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private ServiceRegistration<Servlet> registration;

    @SuppressWarnings("unchecked")
    private Dictionary<String, Object> register(Dictionary<String, Object> config) throws Exception {
        lenient().when(bundleContext.registerService(eq(Servlet.class), any(Servlet.class), any()))
                .thenReturn(registration);

        new ServletExporter(servlet, bundleContext).updated(config);

        ArgumentCaptor<Dictionary<String, Object>> captor = ArgumentCaptor.forClass(Dictionary.class);
        verify(bundleContext).registerService(eq(Servlet.class), eq(servlet), captor.capture());
        return captor.getValue();
    }

    @Test
    public void testServiceListPageIsHiddenByDefault() throws Exception {
        assertEquals("true", register(new Hashtable<>()).get("servlet.init.hide-service-list-page"),
                "the endpoint listing must be opt in");
    }

    @Test
    public void testServiceListPageIsHiddenWhenThereIsNoConfigurationAtAll() throws Exception {
        // ConfigAdmin calls updated(null) when no org.apache.cxf.osgi configuration exists,
        // which is the shape a default install actually takes
        assertEquals("true", register(null).get("servlet.init.hide-service-list-page"),
                "the null config path must use the same default");
    }

    @Test
    public void testServiceListPageCanStillBeTurnedBackOn() throws Exception {
        Dictionary<String, Object> config = new Hashtable<>();
        config.put("org.apache.cxf.servlet.hide-service-list-page", "false");
        assertEquals("false", register(config).get("servlet.init.hide-service-list-page"),
                "an explicit opt in must still be honoured");
    }

    @Test
    public void testOtherDefaultsAreUnchanged() throws Exception {
        Dictionary<String, Object> props = register(new Hashtable<>());
        assertEquals("true", props.get("servlet.init.disable-address-updates"));
        assertEquals("karaf", props.get("servlet.init.service-list-page-authenticate-realm"));
        assertEquals("false", props.get("servlet.init.use-x-forwarded-headers"));
        assertEquals("/cxf/*", props.get("osgi.http.whiteboard.servlet.pattern"));
    }
}
