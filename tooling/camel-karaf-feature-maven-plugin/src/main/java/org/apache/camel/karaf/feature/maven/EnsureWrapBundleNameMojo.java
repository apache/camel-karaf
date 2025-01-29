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
package org.apache.camel.karaf.feature.maven;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.karaf.features.internal.model.Bundle;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "ensure-wrap-bundle-name", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class EnsureWrapBundleNameMojo extends AbstractWrapBundleMojo {

    private static final List<String> HEADERS_AFTER_BUNDLE_NAME = Arrays.asList(
            "Bundle-Version",
            "DynamicImport-Package",
            "Export-Package",
            "Export-Service",
            "Fragment-Host",
            "Import-Package",
            "Import-Service",
            "Provide-Capability",
            "Require-Bundle",
            "Require-Capability");
    
    private static final String BUNDLE_NAME = "Bundle-Name";
    private static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";

    static String toPascalCase(String kebabCase) {
        // Handle null or empty string
        if (kebabCase == null || kebabCase.isEmpty()) {
            return kebabCase;
        }

        // Split by hyphen
        String[] words = kebabCase.split("-");
        StringBuilder result = new StringBuilder();

        // Capitalize first letter of each word
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) {
                continue;
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1).toLowerCase());
            }
            // Only add %20 if it's not the last word
            if (i < words.length - 1) {
                result.append("%20");
            }
        }
        return result.toString();
    }

    @Override
    protected boolean processWrappedBundle(WrappedBundle wrappedBundle) {
        Bundle bundle = wrappedBundle.getBundle();
        String location = bundle.getLocation();
        try {
            bundle.setLocation(processLocation(wrappedBundle));
        } catch (Exception e) {
            getLog().error("Could not process the Bundle location '%s': %s".formatted(location, e.getMessage()), e);
        }
        return false;
    }

    String processLocation(WrappedBundle wrappedBundle) throws Exception {
        String location = wrappedBundle.getBundle().getLocation();
        String instructions = wrappedBundle.getInstructions();
        AtomicBoolean dollarNeeded = new AtomicBoolean(!(instructions!=null && instructions.contains("$")));

        String bundleNameHeader = "%s=Wrap%%20of%%20%s".formatted(BUNDLE_NAME, toPascalCase(wrappedBundle.getArtifactId()));
        String bundleSymbolicNameHeader = "%s=wrap_%s.%s".formatted(BUNDLE_SYMBOLIC_NAME, wrappedBundle.getGroupId(), wrappedBundle.getArtifactId());

        location = insertHeaderIfNeeded(dollarNeeded, location, BUNDLE_NAME, bundleNameHeader);
        return insertHeaderIfNeeded(dollarNeeded, location, BUNDLE_SYMBOLIC_NAME, bundleSymbolicNameHeader);
    }

    private String insertHeaderIfNeeded(AtomicBoolean dollarNeeded, String location, String headerName, String fullHeader) {
        if (location.contains(headerName)) {
            return location;
        }
        StringBuilder sb = new StringBuilder(location);

        // insert before existing headers header
        for (String header : HEADERS_AFTER_BUNDLE_NAME) {
            // add Bundle-Version before
            if (location.contains(header)) {
                int versionHeaderStartIndex = location.indexOf(header);
                if (dollarNeeded.get()) {
                    // "amp;" is automatically added
                    dollarNeeded.set(false);
                    return sb.insert(versionHeaderStartIndex, "$%s&".formatted(fullHeader)).toString();
                } else {
                    // "amp;" is automatically added
                    return sb.insert(versionHeaderStartIndex, "%s&".formatted(fullHeader)).toString();
                }
            }
        }

        // insert at the end
        if (dollarNeeded.get()) {
            dollarNeeded.set(false);
            return sb.insert(location.length(), "$%s".formatted(fullHeader)).toString();
        } else {
            // "amp;" is automatically added
            return sb.insert(location.length(), "&%s".formatted(fullHeader)).toString();
        }
    }
}