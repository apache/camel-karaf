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

package org.apache.camel.karaf.feature.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.features.internal.model.Bundle;

public class WrappedBundle {

    private static final Pattern WRAP_PROTOCOL = Pattern.compile("wrap:mvn:([^/]+)/([^/]+)/([^$]+|\\$\\{[^}]+})(\\$.*)?");

    /**
     * The group id of the bundle
     */
    private final String groupId;
    /**
     * The artifact id of the bundle
     */
    private final String artifactId;
    /**
     * The version of the bundle
     */
    private final String version;
    /**
     * The additional wrapping instructions provided in the WRAP URL
     */
    private final String instructions;
    /**
     * The underlying bundle
     */
    private final Bundle bundle;

    private WrappedBundle(String groupId, String artifactId, String version, String options, Bundle bundle) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.instructions = options;
        this.bundle = bundle;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getInstructions() {
        return instructions;
    }

    public Bundle getBundle() {
        return bundle;
    }

    /**
     * Create a WrappedBundle from the given bundle.
     * @return the WrappedBundle or {@code null} if the bundle is not a wrapped bundle
     */
    public static WrappedBundle fromBundle(Bundle bundle) {
        String location = bundle.getLocation();
        if (location == null) {
            return null;
        }
        return fromLocation(location, bundle);
    }

    private static WrappedBundle fromLocation(String location, Bundle bundle) {
        Matcher matcher = WRAP_PROTOCOL.matcher(location);
        if (matcher.matches()) {
            return new WrappedBundle(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), bundle);
        }
        return null;
    }
}
