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

package org.apache.camel.component.cxf.blueprint.jaxrs.client;

import java.net.URL;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.Namespaces;
import org.apache.aries.blueprint.ParserContext;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

@Namespaces("http://cxf.apache.org/blueprint/jaxrs-client")
public class JAXRSBPNamespaceHandler implements NamespaceHandler {
    private BlueprintContainer blueprintContainer;
    private final org.apache.camel.component.cxf.blueprint.jaxrs.JAXRSBPNamespaceHandler jaxRSBPNamespaceHandler;

    public JAXRSBPNamespaceHandler() {
        jaxRSBPNamespaceHandler = new org.apache.camel.component.cxf.blueprint.jaxrs.JAXRSBPNamespaceHandler();
    }

    @Override
    public URL getSchemaLocation(String namespace) {
        if ("http://cxf.apache.org/blueprint/jaxrs-client".equals(namespace)) {
            return getClass().getClassLoader().getResource("schemas/blueprint/jaxrs-client.xsd");
        }
        return jaxRSBPNamespaceHandler.getSchemaLocation(namespace);
    }


    @Override
    public Metadata parse(Element element, ParserContext context) {
        String s = element.getLocalName();
        if ("client".equals(s)) {
            return new JAXRSClientFactoryBeanDefinitionParser().parse(element, context);
        }
        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set<Class> getManagedClasses() {
        return Set.of();
    }
    @Override
    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        return null;
    }


    public BlueprintContainer getBlueprintContainer() {
        return blueprintContainer;
    }

    public void setBlueprintContainer(BlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

}
