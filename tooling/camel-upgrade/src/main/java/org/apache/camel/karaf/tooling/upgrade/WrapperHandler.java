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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import static org.apache.camel.karaf.tooling.upgrade.Utils.replaceFileContent;

public abstract class WrapperHandler {

    private static final Pattern MODULE_IN_PARENT_POM = Pattern.compile("<module>(.*)</module>");

    protected final Path camelKarafComponentRoot;
    protected final Path camelComponentRoot;
    protected final String camelVersion;

    protected WrapperHandler(Path camelKarafComponentRoot, Path camelComponentRoot, String camelVersion) {
        this.camelKarafComponentRoot = camelKarafComponentRoot;
        this.camelComponentRoot = camelComponentRoot;
        this.camelVersion = camelVersion;
    }

    protected void createModuleIfAbsent(String module) throws IOException {
        Path path = camelKarafComponentRoot.resolve(module);
        if (Files.exists(path)) {
            return;
        }
        Files.createDirectory(path);
    }

    protected void removeModuleIfPresent(String module) throws IOException {
        Path path = camelKarafComponentRoot.resolve(module);
        if (Files.exists(path)) {
            FileUtils.deleteDirectory(path.toFile());
        }
    }

    protected void removeModuleFromParentPom(String component, String path) throws IOException {
        replaceFileContent(camelKarafComponentRoot.resolve(path), pom -> getParentPomWithoutComponent(pom, component));
    }

    protected static String getParentPomWithoutComponent(String pom, String component) {
        String pattern = "<module>%s</module>".formatted(component);
        int index = pom.indexOf(pattern);
        if (index == -1) {
            // Not in the parent pom
            return pom;
        }
        int next = pom.indexOf("<module>", index + 1);
        StringBuilder result = new StringBuilder();
        if (next == -1) {
            next = index + pattern.length();
            String endTag = "</module>";
            int previous = pom.lastIndexOf(endTag, index) + endTag.length();
            result.append(pom, 0, previous);
            result.append(pom, next, pom.length());
        } else {
            result.append(pom, 0, index);
            result.append(pom, next, pom.length());
        }
        return result.toString();
    }

    protected void addModuleToParentPom(String component) throws IOException {
        replaceFileContent(camelKarafComponentRoot.resolve("pom.xml"), pom -> getParentPomWithComponent(pom, component));
    }

    protected static String getParentPomWithComponent(String pom, String component) {
        Matcher matcher = MODULE_IN_PARENT_POM.matcher(pom);
        StringBuilder result = new StringBuilder();
        int start = 0;
        boolean added = false;
        while (matcher.find(start)) {
            String module = matcher.group(1);
            if (module.equals(component)) {
                // Already in the parent pom
                return pom;
            } else if (module.equals("NA")) {
                result.append(pom, 0, matcher.start());
                result.append("<module>%s</module>".formatted(component));
                added = true;
                start = matcher.end();
                break;
            } else if (module.compareTo(component) > 0) {
                result.append(pom, 0, matcher.start());
                result.append("<module>%s</module>%n        ".formatted(component));
                added = true;
                start = matcher.start();
                break;
            }
            start = matcher.end();
        }
        if (!added) {
            result.append(pom, 0, start);
            result.append("%n        <module>%s</module>".formatted(component));
        }
        result.append(pom, start, pom.length());
        return result.toString();
    }

    protected static String getComponentNameFromId(String componentId) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String part : componentId.substring("camel-".length()).split("-")) {
            if (first) {
                first = false;
            } else {
                result.append(' ');
            }
            result.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return result.toString();
    }
}
