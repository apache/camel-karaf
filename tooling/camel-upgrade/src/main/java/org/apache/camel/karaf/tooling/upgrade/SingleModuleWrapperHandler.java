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

    public void add(String component) throws IOException {
        createModuleIfAbsent(component);
        createSingleModulePom(component);
        addModuleToParentPom(component);
    }

    private void createSingleModulePom(String component) throws IOException {
        Files.writeString(camelKarafComponentRoot.resolve(component).resolve("pom.xml"),
                getSingleModulePom(component));
    }

    private String getSingleModulePom(String component) throws IOException {
        return SINGLE_MODULE_POM.replace("#{camel-version}", camelVersion)
                .replace("#{camel-component-id}", component)
                .replace("#{camel-component-name}", getComponentName(component));
    }

    private String getComponentName(String component) throws IOException {
        Path pom = camelComponentRoot.resolve(component).resolve("pom.xml");
        String result = null;
        if (Files.exists(pom)) {
            result = getComponentNameFomPom(pom);
        }
        if (result == null) {
            result = getComponentNameFromId(component);
        }
        return result;
    }

    private static String getComponentNameFomPom(Path pom) throws IOException {
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(pom.toFile()));
            String name = model.getName();
            int index = name.lastIndexOf("::");
            if (index == -1) {
                return null;
            }
            return name.substring(index + 2).trim();
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    public void remove(String component) throws IOException {
        removeModuleIfPresent(component);
        removeModuleFromParentPom(component, "pom.xml");
    }
}
