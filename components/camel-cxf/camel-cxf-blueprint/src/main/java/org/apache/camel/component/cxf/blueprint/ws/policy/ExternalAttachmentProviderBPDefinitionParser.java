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

import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.camel.component.cxf.blueprint.configuration.AbstractBPBeanDefinitionParser;
import org.apache.cxf.ws.policy.attachment.external.ExternalAttachmentProvider;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.Metadata;

public class ExternalAttachmentProviderBPDefinitionParser extends AbstractBPBeanDefinitionParser {

    public Metadata parse(Element element, ParserContext context) {

        MutableBeanMetadata attachmentProvider = context.createMetadata(MutableBeanMetadata.class);
        attachmentProvider.setRuntimeClass(ExternalAttachmentProvider.class);
        if (hasBusProperty()) {
            boolean foundBus = false;
            for (BeanProperty bp : attachmentProvider.getProperties()) {
                if ("bus".equals(bp.getName())) {
                    foundBus = true;
                }
            }
            if (!foundBus) {
                attachmentProvider.addProperty("bus", getBusRef(context, "cxf"));
            }
        }

        parseAttributes(element, context, attachmentProvider);
        parseChildElements(element, context, attachmentProvider);
        return attachmentProvider;
    }

    @Override
    protected boolean hasBusProperty() {
        return true;
    }
}
