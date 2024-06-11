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

import org.apache.felix.utils.version.VersionCleaner;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.osgi.framework.Version;

@Mojo(name = "ensure-wrap-bundle-version", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class EnsureWrapBundleVersionMojo extends AbstractWrapBundleMojo {

    private static final List<String> HEADERS_AFTER_BUNDLE_VERSION = Arrays.asList(
            "DynamicImport-Package",
            "Export-Package",
            "Export-Service",
            "Fragment-Host",
            "Import-Package",
            "Import-Service",
            "Provide-Capability",
            "Require-Bundle",
            "Require-Capability");
    
    static final String BUNDLE_VERSION = "Bundle-Version";

    @Override
    protected boolean processWrappedBundle(Bundle bundle) {
        String location = bundle.getLocation();
        try {
            bundle.setLocation(processLocation(location));
        } catch (Exception e) {
            getLog().error("Could not process the Bundle location '%s': %s".formatted(location, e.getMessage()), e);
        }
        return false;
    }

    String processLocation(String location) throws Exception {
        int versionStartIndex = getVersionStartIndex(location);
        int versionEndIndex = getVersionEndIndex(location, versionStartIndex);

        String rawVersion = getVersion(location, versionStartIndex, versionEndIndex);
        String version = getValidVersion(location, rawVersion);

        String bundleVersionHeader = "%s=%s".formatted(BUNDLE_VERSION, version);

        if (location.contains(bundleVersionHeader)) {
            return location;
        } else if (location.contains(BUNDLE_VERSION)) {
            return updateExistingVersion(location, bundleVersionHeader);
        }

        String wrapProtocolOptions = location.substring(versionEndIndex + 1);
        StringBuilder sb = new StringBuilder(location);

        // insert before existing headers header
        for (String header : HEADERS_AFTER_BUNDLE_VERSION) {
            // add Bundle-Version before
            if (location.contains(header)) {
                int versionHeaderStartIndex = location.indexOf(header);
                if (wrapProtocolOptions.contains("$")) {
                    // "amp;" is automatically added
                    return sb.insert(versionHeaderStartIndex, "%s&".formatted(bundleVersionHeader)).toString();
                } else {
                    // "amp;" is automatically added
                    return sb.insert(versionHeaderStartIndex, "$%s&".formatted(bundleVersionHeader)).toString();
                }
            }
        }

        // insert at the end
        if (wrapProtocolOptions.contains("$")) {
            // "amp;" is automatically added
            return sb.insert(location.length(), "&%s".formatted(bundleVersionHeader)).toString();
        } else {
            return sb.insert(location.length(), "$%s".formatted(bundleVersionHeader)).toString();
        }
    }

    /**
     * @return artifact version first char index, inclusive
     */
    int getVersionStartIndex(String location) {
        boolean artifactIdFound = false;
        for (int i = 0; i < location.length(); i++) {
            if ('/' == location.charAt(i)) {
                if (!artifactIdFound) {
                    artifactIdFound = true;
                } else {
                    return i + 1;
                }
            }
        }

        return -1;
    }

    int getVersionEndIndex(String location) {
        return getVersionEndIndex(location, getVersionStartIndex(location));
    }

    /**
     * @return artifact version last char index, inclusive
     */
    int getVersionEndIndex(String location, int versionStartIndex) {
        // start at + 1 to ignore the potential $ coming from version placeholder
        for (int i = versionStartIndex + 1; i < location.length(); i++) {
            if ('$' == location.charAt(i)) {
                return i - 1;
            }
        }

        return location.length() - 1;
    }

    String getVersion(String Location) {
        return getVersion(Location, getVersionStartIndex(Location), getVersionEndIndex(Location));
    }

    String getVersion(String location, int versionStartIndex, int versionEndIndex) {
        return location.substring(versionStartIndex, versionEndIndex + 1);
    }

    String getValidVersion(String location, String version) throws Exception {
        if (version.charAt(0) == '$') {
            throw new Exception("Maven version placeholder '%s' wasn't resolved".formatted(version));
        }

        String cleanVersion = VersionCleaner.clean(version);
        if (getLog().isDebugEnabled()) {
            getLog().debug(
                    "Bundle location '%s' will be set with Bundle-Version '%s', the output of VersionCleaner.clean(%s)"
                            .formatted(location, cleanVersion, version));
        }

        // test if clean version will work in Karaf, just in case
        try {
            new Version(cleanVersion);
            return cleanVersion;
        } catch (Exception newException) {
            throw new Exception("Version '%s' is not OSGi compliant".formatted(cleanVersion), newException);
        }
    }

    String updateExistingVersion(String location, String bundleVersioHeader) throws Exception {
        int versionHeaderStartIndex = location.indexOf(BUNDLE_VERSION);
        int versionHeaderEndIndex = getBundleVersionHeaderEndIndex(location, versionHeaderStartIndex);

        // BUNDLE_VERSION.length() + 1 will include '='
        String currentVersion = location.substring(versionHeaderStartIndex + BUNDLE_VERSION.length() + 1,
                versionHeaderEndIndex + 1);
        if (currentVersion.charAt(0) == '$' || !currentVersion.equals(getValidVersion(location, currentVersion))) {
            String currentBundleVersionHeader = location.substring(versionHeaderStartIndex, versionHeaderEndIndex + 1);

            return location.replace(currentBundleVersionHeader, bundleVersioHeader);
        }

        return location;
    }

    /**
     * @return wrap protocol Bundle-Version header last char index, inclusive
     */
    int getBundleVersionHeaderEndIndex(String location, int versionHeaderStartIndex) {
        for (int i = versionHeaderStartIndex; i < location.length(); i++) {
            if (location.charAt(i) == '&' || location.charAt(i) == ';') {
                return i - 1;
            }
        }

        return location.length() - 1;
    }
}