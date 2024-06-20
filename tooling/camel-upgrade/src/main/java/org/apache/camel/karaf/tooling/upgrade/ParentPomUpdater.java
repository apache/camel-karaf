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
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.karaf.tooling.upgrade.Utils.replaceFileContent;

public class ParentPomUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(ParentPomUpdater.class);

    private static final Pattern DEPENDENCY_VERSION_PATTERN = Pattern.compile("<([^>]+)>([^<]+)</([^>]+)>");

    private final String camelVersion;
    private final Path camelKarafRootPom;
    private final Path camelParentPom;

    public ParentPomUpdater(Path camelKarafRootPom, Path camelParentPom, String camelVersion) {
        this.camelVersion = camelVersion;
        this.camelKarafRootPom = camelKarafRootPom;
        this.camelParentPom = camelParentPom;
    }

    public void execute() throws IOException {
        updateCamelVersion();
        updateCamelOsgiVersionRange();
        updateCamelDependenciesVersion();
    }

    private void updateCamelVersion() throws IOException {
        LOG.debug("Updating the Camel version to {}", camelVersion);
        updatePom(Map.of("camel-version", camelVersion));
    }

    private void updateCamelOsgiVersionRange() throws IOException {
        String cleanVersion = cleanVersion(camelVersion);
        String nextVersion = nextVersion(cleanVersion);
        LOG.debug("Updating the Camel OSGi version to {}", cleanVersion);
        LOG.debug("Updating the Camel OSGi next version to {}", nextVersion);
        updatePom(Map.of("camel-osgi-version-clean", cleanVersion, "camel-osgi-next-version-clean", nextVersion));
    }

    private void updateCamelDependenciesVersion() throws IOException {
        LOG.debug("Updating the version of the Camel dependencies");
        setDependenciesVersion(extractDependenciesVersion(camelParentPom));
    }

    private void updatePom(Map<String, String> changes) throws IOException {
        replaceFileContent(camelKarafRootPom,
                pom -> {
                    for (Map.Entry<String, String> entry : changes.entrySet()) {
                        pom = pom.replaceFirst("<%s>(.*)</%s>".formatted(entry.getKey(), entry.getKey()),
                                "<%s>%s</%s>".formatted(entry.getKey(), entry.getValue(), entry.getKey()));
                    }
                    return pom;
                }
        );
    }

    private void setDependenciesVersion(String dependencies) throws IOException {
        replaceFileContent(camelKarafRootPom, pom -> getPomWithDependenciesVersion(pom, dependencies));
    }

    private static String getPomWithDependenciesVersion(String pom, String dependencies) {
        String startPattern = "<!-- START: Maven Properties defining the version of 3rd party libraries used in Camel -->";
        int start = pom.indexOf(startPattern);
        if (start == -1) {
            throw new IllegalStateException("Unable to find the start position in parent pom file of Camel Karaf");
        }
        int end = pom.indexOf("<!-- END: Maven Properties defining the version of 3rd party libraries used in Camel -->", start);
        if (end == -1) {
            throw new IllegalStateException("Unable to find the end position in parent pom file of Camel");
        }
        StringBuilder result = new StringBuilder();
        result.append(pom, 0, start);
        result.append(startPattern);
        result.append('\n');
        result.append(dependencies);
        result.append("        ");
        result.append(pom, end, pom.length());
        return result.toString();
    }

    private static String extractDependenciesVersion(Path path) throws IOException {
        String pom = Files.readString(path, StandardCharsets.UTF_8);
        String startPattern = "<!-- dependency versions -->";
        int start = pom.indexOf(startPattern);
        if (start == -1) {
            throw new IllegalStateException("Unable to find the start position in parent pom file of Camel");
        }
        int end = pom.indexOf("</properties>", start);
        if (end == -1) {
            throw new IllegalStateException("Unable to find the end position in parent pom file of Camel");
        }
        return extractVersionsOnly(pom.substring(start + startPattern.length(), end));
    }

    private static String extractVersionsOnly(String content) {
        StringBuilder result = new StringBuilder();
        var matcher = DEPENDENCY_VERSION_PATTERN.matcher(content);
        int start = 0;
        while (matcher.find(start)) {
            String tagName = matcher.group(1);
            if (acceptTag(tagName)) {
                tagName = tagName.replace('.', '-');
                result.append("        <%s>%s</%s>%n".formatted(tagName, matcher.group(2), tagName));
            }
            start = matcher.end();
        }
        return result.toString();
    }

    private static boolean acceptTag(String tagName) {
        return !tagName.contains("maven");
    }

    private static String nextVersion(String version) {
        String[] parts = version.split("\\.");
        int last = Integer.parseInt(parts[parts.length - 1]);
        parts[parts.length - 1] = String.valueOf(last + 1);
        return String.join(".", parts);
    }

    private static String cleanVersion(String version) {
        return version.substring(0, version.lastIndexOf('.'));
    }
}
