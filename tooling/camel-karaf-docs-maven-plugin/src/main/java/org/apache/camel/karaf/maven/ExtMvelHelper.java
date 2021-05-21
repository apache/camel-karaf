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
package org.apache.camel.karaf.maven;

import java.nio.file.Path;

import org.apache.camel.maven.packaging.MvelHelper;
import org.apache.camel.tooling.model.OtherModel;
import org.apache.camel.tooling.util.Strings;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.LanguageModel;

public class ExtMvelHelper {

    private final Path extensionsDocPath;

    public ExtMvelHelper(Path extensionsDocPath) {
        this.extensionsDocPath = extensionsDocPath;
    }

    public static String escape(final String raw) {
        return MvelHelper.escape(raw);
    }

    public String getFirstVersionShort(Object model) {
        String version = (String) invokeGetter(model, "getFirstVersion");
        return org.apache.camel.tooling.model.Strings.cutLastZeroDigit(version);
    }

    public String getDocLink(Object model) {
        if (localDocExists(model)) {
            return getLocalDocLink(model);
        } else if (model instanceof ComponentModel) {
            final ComponentModel component = (ComponentModel) model;
            final String scheme = component.getScheme();
            if ("org.apache.camel.karaf".equals(component.getGroupId())) {
                return String.format("xref:3.10.x@camel-karaf::%s-component.adoc", scheme);
            } else {
                return String.format("xref:3.10.x@components::%s-component.adoc", scheme);
            }
        } else if (model instanceof DataFormatModel) {
            return String.format("xref:3.10.x@components:dataformats:%s-dataformat.adoc",
                    invokeGetter(model, "getName"));
        } else if (model instanceof LanguageModel) {
            return String.format("xref:3.10.x@components:languages:%s-language.adoc",
                    invokeGetter(model, "getName"));
        } else if (model instanceof OtherModel) {
            final OtherModel other = (OtherModel) model;
            final String name = other.getName();
            if ("org.apache.camel.karaf".equals(other.getGroupId())) {
                return String.format("xref:3.10.x@camel-karaf::%s.adoc", name);
            } else {
                return String.format("xref:3.10.x@components:others:%s.adoc", name);
            }
        } else {
            return null;
        }
    }

    private Object invokeGetter(Object model, String method) {
        try {
            return model.getClass().getMethod(method)
                    .invoke(model);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access " + method + " from " + model, e);
        }
    }

    private boolean localDocExists(Object model) {
        Path path = extensionsDocPath.resolve(getSpringBootDocName(model));
        return path.toFile().exists();
    }

    private String getLocalDocLink(Object model) {
        return "xref:components-starter/" + getSpringBootDocName(model);
    }

    private String getSpringBootDocName(Object model) {
        return Strings.after((String) invokeGetter(model, "getArtifactId"), "camel-") + ".adoc";
    }
}
