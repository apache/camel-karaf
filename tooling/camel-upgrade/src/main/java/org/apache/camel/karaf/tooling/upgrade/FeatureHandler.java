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
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.camel.karaf.tooling.upgrade.Utils.nextLineIndex;
import static org.apache.camel.karaf.tooling.upgrade.Utils.replaceFileContent;

public class FeatureHandler {

    private static final Pattern FEATURE_IN_FEATURES_XML = Pattern.compile("<feature name=['\"]([^'\"]+)['\"]");
    private static final String FEATURE_FORMAT = """
    <feature name='#{camel-component-id}' version='${project.version}' start-level='50'>
            <feature version='${camel-osgi-version-range}'>camel-core</feature>
            <bundle>mvn:org.apache.camel.karaf/#{camel-component-id}/${project.version}</bundle>
        </feature>
    """;

    private final Path camelKarafFeatures;

    public FeatureHandler(Path camelKarafFeatures) {
        this.camelKarafFeatures = camelKarafFeatures;
    }

    public void add(String component) throws IOException {
        addComponentToFeatures(component);
    }

    private void addComponentToFeatures(String component) throws IOException {
        replaceFileContent(camelKarafFeatures, features -> getFeaturesWithComponent(features, component));
    }

    private static String getFeaturesWithComponent(String features, String component) {
        int start = features.indexOf("<!-- the following features are sorted A..Z -->");
        if (start == -1) {
            throw new IllegalStateException("Unable to find the start position in the features files");
        }
        Matcher matcher = FEATURE_IN_FEATURES_XML.matcher(features);
        StringBuilder result = new StringBuilder();
        boolean added = false;
        while (matcher.find(start)) {
            String feature = matcher.group(1);
            if (feature.equals(component)) {
                // Already in the features
                return features;
            } else if (feature.compareTo(component) > 0) {
                result.append(features, 0, matcher.start());
                result.append(FEATURE_FORMAT.replace("#{camel-component-id}", component));
                result.append("    ");
                added = true;
                start = matcher.start();
                break;
            }
            start = matcher.end();
        }
        if (!added) {
            start = nextLineIndex(features, features.lastIndexOf("</feature>"));
            result.append(features, 0, start);
            result.append("    ");
            result.append(FEATURE_FORMAT.replace("#{camel-component-id}", component));
        }
        result.append(features, start, features.length());
        return result.toString();
    }

    public void remove(String component) throws IOException {
        removeComponentFromFeatures(component);
    }

    private void removeComponentFromFeatures(String component) throws IOException {
        replaceFileContent(camelKarafFeatures, features -> getFeaturesWithoutComponent(features, component));
    }

    private static String getFeaturesWithoutComponent(String features, String component) {
        int index = features.indexOf("<feature name='%s'".formatted(component));
        if (index == -1) {
            // Not in the features
            return features;
        }
        int next = features.indexOf("<feature name='camel-", index + 1);
        StringBuilder result = new StringBuilder();
        if (next == -1) {
            String endTag = "</feature>";
            next = features.lastIndexOf(endTag) + endTag.length();
            int previous = features.lastIndexOf(endTag, index) + endTag.length();
            result.append(features, 0, previous);
            result.append(features, next, features.length());
        } else {
            result.append(features, 0, index);
            result.append(features, next, features.length());
        }
        return result.toString();
    }
}
