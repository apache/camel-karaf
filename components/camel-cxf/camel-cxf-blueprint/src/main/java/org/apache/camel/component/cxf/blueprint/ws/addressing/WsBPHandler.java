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

package org.apache.camel.component.cxf.blueprint.ws.addressing;

import java.net.URL;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.camel.component.cxf.blueprint.configuration.SimpleBPBeanDefinitionParser;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

public class WsBPHandler implements NamespaceHandler {

    @Override
    public URL getSchemaLocation(String s) {
        if ("http://cxf.apache.org/ws/addressing".equals(s)) {
            return getClass().getClassLoader().getResource("schemas/ws-addr-conf.xsd");
        }
        // no imported XSDs, so we don't have to delegate to cxf-core namespace handler
        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set<Class> getManagedClasses() {
        return Set.of();
    }

    @Override
    public Metadata parse(Element element, ParserContext context) {
        return new SimpleBPBeanDefinitionParser(WSAddressingFeature.class).parse(element, context);
    }

    @Override
    public ComponentMetadata decorate(Node node, ComponentMetadata componentMetadata, ParserContext context) {
        return null;
    }
}
