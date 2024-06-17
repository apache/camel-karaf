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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public abstract class ComponentComparator {

    private final Path camelKarafComponentRoot;
    private final Path camelComponentRoot;

    protected ComponentComparator(Path camelKarafComponentRoot, Path camelComponentRoot) {
        this.camelKarafComponentRoot = camelKarafComponentRoot;
        this.camelComponentRoot = camelComponentRoot;
    }

    private static boolean isCamelComponentDirectory(File file) {
        return file.isDirectory() && file.getName().startsWith("camel-");
    }

    static boolean isCamelSubComponentDirectory(File file) {
        return isCamelComponentDirectory(file)
                && !file.getName().endsWith("-codegen")
                && !file.getName().endsWith("-maven-plugin")
                && !file.getName().endsWith("-spi")
                && !file.getName().endsWith("-api");
    }

    private static List<String> getComponentsInFolder(Path root) throws IOException {
        File[] files = root.toFile().listFiles(ComponentComparator::isCamelSubComponentDirectory);
        if (files == null) {
            throw new IOException("Unable to list the camel sub component in %s".formatted(root));
        }
        if (files.length == 0) {
            return List.of(root.getFileName().toString());
        } else if (files.length == 1) {
            return List.of(files[0].getName().replace("-component", ""));
        } else {
            return Stream.of(files)
                    .map(file -> file.getName().replace("-component", ""))
                    .map(name -> "%s/%s".formatted(root.getFileName(), name))
                    .toList();
        }
    }

    private static Set<String> getComponents(Path root) throws IOException {
        File[] files = root.toFile().listFiles(ComponentComparator::isCamelComponentDirectory);
        if (files == null) {
            throw new IOException("Unable to list the camel component in %s".formatted(root));
        }
        List<String> components = new ArrayList<>();
        for (File file : files) {
            components.addAll(getComponentsInFolder(file.toPath()));
        }
        components.sort(String::compareTo);
        return new LinkedHashSet<>(components);
    }

    public void execute() throws IOException {
        Set<String> componentsInCamelKaraf = getComponents(camelKarafComponentRoot);
        Set<String> componentsInCamel = getComponents(camelComponentRoot);
        beforeAddingComponents();
        for (String component : componentsInCamel) {
            if (componentsInCamelKaraf.contains(component)) {
                // The component is already in Camel Karaf
                componentsInCamelKaraf.remove(component);
            } else {
                int index = component.indexOf('/');
                if (index == -1) {
                    onAddComponent(component);
                } else {
                    onAddSubComponent(component.substring(0, index), component.substring(index + 1));
                }
            }
        }
        beforeRemovingComponents();
        for (String component : componentsInCamelKaraf) {
            int index = component.indexOf('/');
            if (index == -1) {
                onRemoveComponent(component);
            } else {
                onRemoveSubComponent(component.substring(0, index), component.substring(index + 1));
            }
        }
    }

    protected void beforeRemovingComponents() {
        // Do nothing by default
    }

    protected void beforeAddingComponents() {
        // Do nothing by default
    }

    protected abstract void onAddSubComponent(String parent, String subComponent) throws IOException;

    protected abstract void onAddComponent(String component) throws IOException;

    protected abstract void onRemoveSubComponent(String parent, String subComponent) throws IOException;

    protected abstract void onRemoveComponent(String component) throws IOException;
}
