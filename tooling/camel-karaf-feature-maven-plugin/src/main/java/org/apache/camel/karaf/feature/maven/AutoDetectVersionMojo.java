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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.osgi.framework.Version;

@Mojo(name = "auto-detect-version", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class AutoDetectVersionMojo extends AbstractFeaturesMojo {

    private static final String AUTO_DETECT_PLACEHOLDER_PREFIX = "${auto-detect-version";
    private static final Pattern AUTO_DETECT_PLACEHOLDER = Pattern.compile("\\$\\{auto-detect-version(:alias=([^/]+)/([^}]+))?}");

    private static final Pattern MVN_BASED_PROTOCOL = Pattern.compile("(wrap:)?mvn:([^/]+)/([^/]+)/([^$]+|\\$\\{auto-detect-version(:[^}]+)?}[^$]*)(\\$.*)?");

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repositories;

    @Override
    protected void processFeature(Feature feature) {
        List<Bundle> bundlesToProcess = new ArrayList<>();
        List<Bundle> roots = new ArrayList<>();
        for (Bundle bundle : feature.getBundle()) {
            if (containsPlaceholder(bundle)) {
                bundlesToProcess.add(bundle);
            } else {
                roots.add(bundle);
            }
        }
        if (bundlesToProcess.isEmpty()) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("No bundles found in the feature %s with the auto-detect version placeholder".formatted(feature.getName()));
            }
            return;
        }
        if (roots.isEmpty()) {
            getLog().error("No root bundles found in the feature %s".formatted(feature.getName()));
            return;
        }
        autoDetectVersion(feature, roots, bundlesToProcess);
    }

    /**
     * Auto-detect the version of the bundles to process according to the dependencies of the given root bundles.
     *
     * @param feature the feature that contains the bundles
     * @param roots the root bundles from which the dependencies are resolved
     * @param bundlesToProcess the bundles for which the version should be auto-detected
     */
    private void autoDetectVersion(Feature feature, List<Bundle> roots, List<Bundle> bundlesToProcess) {
        Map<String, BundleVersion> dependencies = resolveDependencies(roots);
        if (dependencies.isEmpty()) {
            getLog().error("No dependencies found for the root bundles in the feature %s".formatted(feature.getName()));
            return;
        }
        for (Bundle bundle : bundlesToProcess) {
            autoDetectVersion(feature, bundle, dependencies);
        }
    }

    /**
     * Auto-detect the version of the given bundle according to the provided dependencies.
     *
     * @param feature the feature that contains the bundle
     * @param bundle the bundle for which the version should be auto-detected
     * @param dependencies the dependencies to use for the auto-detection where the key is the group id / artifact id
     *                     and the value is the version
     */
    private void autoDetectVersion(Feature feature, Bundle bundle, Map<String, BundleVersion> dependencies) {
        String location = bundle.getLocation();
        Matcher matcher = MVN_BASED_PROTOCOL.matcher(location);
        if (!matcher.matches()) {
            getLog().warn("Bundle location %s does not match with a maven based protocol in the feature %s".formatted(location, feature.getName()));
            return;
        }
        final String groupId;
        final String artifactId;
        Matcher aliasMatcher = AUTO_DETECT_PLACEHOLDER.matcher(matcher.group(4));
        if (!aliasMatcher.find()) {
            getLog().warn("Bundle location %s does not match with a placeholder syntax in the feature %s".formatted(location, feature.getName()));
            return;
        }
        if (aliasMatcher.group(2) != null && aliasMatcher.group(3) != null) {
            groupId = aliasMatcher.group(2);
            artifactId = aliasMatcher.group(3);
            if (getLog().isDebugEnabled()) {
                getLog().debug("Alias %s/%s detected for the artifact %s in the feature %s".formatted(groupId, artifactId, location, feature.getName()));
            }
        } else {
            groupId = matcher.group(2);
            artifactId = matcher.group(3);
        }
        BundleVersion version = dependencies.get("%s/%s".formatted(groupId, artifactId));
        if (version == null) {
            getLog().error("Version of the artifact %s/%s could not be auto-detected in the feature %s".formatted(groupId, artifactId, feature.getName()));
            return;
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Version %s detected for the artifact %s/%s in the feature %s".formatted(version, groupId, artifactId, feature.getName()));
        }
        bundle.setLocation(AUTO_DETECT_PLACEHOLDER.matcher(location).replaceAll(version.toString()));
    }

    /**
     * Resolve the dependencies of the given root bundles. In case of conflicts, the highest version is kept.
     *
     * @param roots the root bundles from which the dependencies are resolved
     * @return the dependencies of the given root bundles where the key is the group id / artifact id and the value is
     * the version
     */
    private Map<String, BundleVersion> resolveDependencies(List<Bundle> roots) {
        Map<String, BundleVersion> dependencies = new HashMap<>();
        for (Bundle root : roots) {
            putAllDependencies(dependencies, resolveDependencies(root));
        }
        return dependencies;
    }

    /**
     * Put all the dependencies in the given map. In case of conflicts, the highest version is kept.
     *
     * @param all the map to fill with the dependencies
     * @param dependencies the dependencies to put in the map
     */
    private void putAllDependencies(Map<String, BundleVersion> all, Map<String, BundleVersion> dependencies) {
        for (Map.Entry<String, BundleVersion> entry : dependencies.entrySet()) {
            all.compute(entry.getKey(), (k, v) -> v == null || v.compareTo(entry.getValue()) < 0 ? entry.getValue() : v);
        }
    }

    /**
     * Resolve the dependencies of the given root bundle. In case of conflicts, the highest version is kept.
     *
     * @param root the root bundle from which the dependencies are resolved
     * @return the dependencies of the given root bundle where the key is the group id / artifact id and the value is
     * the version
     */
    private Map<String, BundleVersion> resolveDependencies(Bundle root) {
        String location = root.getLocation();
        if (location == null) {
            getLog().warn("Root bundle location is null");
            return Map.of();
        }
        Matcher matcher = MVN_BASED_PROTOCOL.matcher(location);
        if (!matcher.matches()) {
            getLog().warn("Root bundle location %s is not a Maven location".formatted(location));
            return Map.of();
        }
        List<Artifact> artifacts = resolveDependencies(matcher.group(2), matcher.group(3), matcher.group(4));
        if (artifacts.isEmpty()) {
            return Map.of();
        }
        Map<String, BundleVersion> dependencies = new HashMap<>();
        for (Artifact artifact : artifacts) {
            putArtifact(dependencies, artifact);
        }
        return dependencies;
    }

    /**
     * Put the artifact in the given map. In case of conflicts, the highest version is kept.
     *
     * @param dependencies the map to fill with the artifact
     * @param artifact the artifact to put in the map
     */
    private static void putArtifact(Map<String, BundleVersion> dependencies, Artifact artifact) {
        dependencies.compute("%s/%s".formatted(artifact.getGroupId(), artifact.getArtifactId()),
            (k, v) -> {
                BundleVersion v2 = BundleVersion.parseVersion(artifact.getVersion());
                if (v == null) {
                    return v2;
                }
                return v.compareTo(v2) >= 0 ? v : v2;
        });
    }

    /**
     * Resolve the dependencies of the given maven coordinates.
     *
     * @param groupId the group id of the artifact for which the dependencies should be resolved
     * @param artifactId the artifact id of the artifact for which the dependencies should be resolved
     * @param version the version of the artifact for which the dependencies should be resolved
     * @return the dependencies of the artifact corresponding to the given maven coordinates
     */
    private List<Artifact> resolveDependencies(String groupId, String artifactId, String version) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Resolving the dependencies of the artifact %s/%s/%s".formatted(groupId, artifactId, version));
        }
        DependencyRequest req = new DependencyRequest()
                .setCollectRequest(new CollectRequest()
                        .setRoot(new Dependency(new DefaultArtifact(groupId, artifactId, "jar", version), "runtime"))
                        .setRepositories(this.repositories));
        try {
            return this.repoSystem.resolveDependencies(this.repoSession, req)
                    .getArtifactResults()
                    .stream()
                    .map(ArtifactResult::getArtifact)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            getLog().warn("Dependencies of the artifact %s could not be resolved: %s".formatted(artifactId, e.getMessage()));
            if (getLog().isDebugEnabled()) {
                getLog().debug(e);
            }
        }
        return List.of();
    }

    /**
     * Indicates whether the given bundle contains the auto-detect version placeholder.
     *
     * @param bundle the bundle to check
     * @return {@code true} if the given bundle contains the auto-detect version placeholder, {@code false} otherwise
     */
    private static boolean containsPlaceholder(Bundle bundle) {
        String location = bundle.getLocation();
        return location != null && location.contains(AUTO_DETECT_PLACEHOLDER_PREFIX);
    }

    /**
     * Represents a bundle version that can be compared even if the version is not a valid OSGi version.
     */
    private record BundleVersion(String originalVersion, Version version) implements Comparable<BundleVersion> {

        static BundleVersion parseVersion(String version) {
            try {
                return new BundleVersion(version, Version.parseVersion(version));
            } catch (IllegalArgumentException e) {
                // The version is not a valid OSGi version
                return new BundleVersion(version, null);
            }
        }

        @Override
        public int compareTo(BundleVersion other) {
            if (this.version == null || other.version == null) {
                return this.originalVersion.compareTo(other.originalVersion);
            }
            return this.version.compareTo(other.version);
        }

        @Override
        public String toString() {
            return originalVersion;
        }
    }
}
