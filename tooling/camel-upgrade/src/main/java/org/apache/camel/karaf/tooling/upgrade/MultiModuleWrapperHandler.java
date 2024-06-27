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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.camel.karaf.tooling.upgrade.Utils.loadFileFromClassLoader;
import static org.apache.camel.karaf.tooling.upgrade.Utils.replaceFileContent;

public class MultiModuleWrapperHandler extends WrapperHandler {

    private static final String MULTI_MODULE_POM = loadFileFromClassLoader("/multi-module-pom.xml");
    private static final String MULTI_MODULE_PARENT_POM = loadFileFromClassLoader("/multi-module-parent-pom.xml");

    public MultiModuleWrapperHandler(Path camelKarafComponentRoot, Path camelComponentRoot, String camelVersion) {
        super(camelKarafComponentRoot, camelComponentRoot, camelVersion);
    }

    public void add(String originalSubComponentPath, String parent, String subComponent) throws IOException {
        createModuleIfAbsent(parent);
        createModuleIfAbsent("%s/%s".formatted(parent, subComponent));
        createMultiModuleParentPomIfAbsent(parent);
        createMultiModulePom(originalSubComponentPath, parent, subComponent);
        addSubModuleToParentPom(parent, subComponent);
        addModuleToParentPom(parent);
    }

    private void createMultiModuleParentPomIfAbsent(String parent) throws IOException {
        Path path = camelKarafComponentRoot.resolve(parent).resolve("pom.xml");
        if (Files.exists(path)) {
            return;
        }
        Files.writeString(path, getMultiModuleParentPom(parent));
    }

    private String getMultiModuleParentPom(String parent) {
        return MULTI_MODULE_PARENT_POM.replace("#{camel-version}", camelVersion)
                .replace("#{camel-parent-component-id}", parent)
                .replace("#{camel-parent-component-name}", getComponentNameFromId(parent));
    }

    private void addSubModuleToParentPom(String parent, String subComponent) throws IOException {
        replaceFileContent(camelKarafComponentRoot.resolve(parent).resolve("pom.xml"), pom -> getParentPomWithComponent(pom, subComponent));
    }

    private void createMultiModulePom(String originalSubComponentPath, String parent, String subComponent) throws IOException {
        Files.writeString(camelKarafComponentRoot.resolve(parent).resolve(subComponent).resolve("pom.xml"),
                getMultiModulePom(originalSubComponentPath, parent, subComponent));
    }

    private String getMultiModulePom(String originalSubComponentPath, String parent, String subComponent) throws IOException {
        return MULTI_MODULE_POM.replace("#{camel-version}", camelVersion)
                .replace("#{camel-component-id}", subComponent)
                .replace("#{camel-component-name}", getComponentName(originalSubComponentPath, subComponent))
                .replace("#{camel-parent-component-id}", parent)
                .replace("#{camel-parent-component-name}", getComponentNameFromId(parent));
    }

    public void remove(String parent, String subComponent) throws IOException {
        removeModuleIfPresent("%s/%s".formatted(parent, subComponent));
        removeModuleFromParentPom(subComponent, "%s/pom.xml".formatted(parent));
        removeMultiModuleIfNeeded(parent);
    }

    private void removeMultiModuleIfNeeded(String parent) throws IOException {
        Path root = camelKarafComponentRoot.resolve(parent);
        File[] files = root.toFile().listFiles(ComponentComparator::isCamelSubComponentDirectory);
        if (files == null) {
            throw new IOException("Unable to list the camel sub component in %s".formatted(root));
        }
        if (files.length == 0) {
            removeModuleIfPresent(parent);
            removeModuleFromParentPom(parent, "pom.xml");
        }
    }
}
