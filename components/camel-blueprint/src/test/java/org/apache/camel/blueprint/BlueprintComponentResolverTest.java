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
package org.apache.camel.blueprint;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.DefaultRegistry;
import org.apache.camel.support.service.ServiceSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlueprintComponentResolverTest {

    @Test
    void testOsgiResolverFindComponentFallbackTest() throws Exception {
        Registry registry = new DefaultRegistry();
        registry.bind("allstar-component", new SampleComponent(true));

        CamelContext camelContext = new DefaultCamelContext(registry);

        BlueprintComponentResolver resolver = new BlueprintComponentResolver(null);
        Component component = resolver.resolveComponent("allstar", camelContext);
        assertNotNull(component, "We should find the super component");
        assertInstanceOf(SampleComponent.class, component, "We should get the super component here");
    }

    @Test
    void testOsgiResolverFindLanguageDoubleFallbackTest() throws Exception {
        Registry registry = new DefaultRegistry();
        registry.bind("allstar", new SampleComponent(false));
        registry.bind("allstar-component", new SampleComponent(true));

        CamelContext camelContext = new DefaultCamelContext(registry);

        BlueprintComponentResolver resolver = new BlueprintComponentResolver(null);
        Component component = resolver.resolveComponent("allstar", camelContext);
        assertNotNull(component, "We should find the super component");
        assertInstanceOf(SampleComponent.class, component, "We should get the super component here");
        assertFalse(((SampleComponent) component).isFallback(), "We should NOT find the fallback component");
    }

    private static class SampleComponent extends ServiceSupport implements Component {

        private boolean fallback;

        SampleComponent(boolean fallback) {
            this.fallback = fallback;
        }

        @Override
        public void setCamelContext(CamelContext camelContext) {
            // noop
        }

        @Override
        public CamelContext getCamelContext() {
            throw new UnsupportedOperationException("Should not be called");
        }

        @Override
        public Endpoint createEndpoint(String uri) throws Exception {
            throw new UnsupportedOperationException("Should not be called");
        }

        @Override
        public Endpoint createEndpoint(String uri, Map<String, Object> parameters) throws Exception {
            throw new UnsupportedOperationException("Should not be called");
        }

        @Override
        public boolean useRawUri() {
            throw new UnsupportedOperationException("Should not be called");
        }

        public boolean isFallback() {
            return fallback;
        }

        public void setFallback(boolean fallback) {
            this.fallback = fallback;
        }

        @Override
        protected void doStart() throws Exception {
            // noop
        }

        @Override
        protected void doStop() throws Exception {
            // noop
        }
    }

}
