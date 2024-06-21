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

package org.apache.camel.karaf.tooling.upgrade;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import static org.apache.camel.karaf.tooling.upgrade.Utils.loadFileFromClassLoader;

public class SingleModuleWrapperHandler extends WrapperHandler {

    private static final String SINGLE_MODULE_POM = loadFileFromClassLoader("/single-module-pom.xml");

    public SingleModuleWrapperHandler(Path camelKarafComponentRoot, Path camelComponentRoot, String camelVersion) {
        super(camelKarafComponentRoot, camelComponentRoot, camelVersion);
    }

    public void add(String originalComponentPath, String component) throws IOException {
        createModuleIfAbsent(component);
        createSingleModulePom(originalComponentPath, component);
        addModuleToParentPom(component);
    }

    private void createSingleModulePom(String originalComponentPath, String component) throws IOException {
        Files.writeString(camelKarafComponentRoot.resolve(component).resolve("pom.xml"),
                getSingleModulePom(originalComponentPath, component));
    }

    private String getSingleModulePom(String originalComponentPath, String component) throws IOException {
        return SINGLE_MODULE_POM.replace("#{camel-version}", camelVersion)
                .replace("#{camel-component-id}", component)
                .replace("#{camel-component-name}", getComponentName(originalComponentPath, component));
    }

    public void remove(String component) throws IOException {
        removeModuleIfPresent(component);
        removeModuleFromParentPom(component, "pom.xml");
    }
}
