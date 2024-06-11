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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Dependency;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;


@Mojo(name = "configure-wrap-spi-provider", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class ConfigureWrapSpiProviderMojo extends AbstractWrapBundleMojo {

    private static final Pattern WRAP_PROTOCOL = Pattern.compile("wrap:mvn:([^/]+)/([^/]+)/([^$]+)(\\$([^=]+=[^&]+)(&([^=]+=[^&]+))*)?");
    private static final String SPI_PROVIDER = "SPI-Provider";
    private static final String SPI_HEADER = "%s=*".formatted(SPI_PROVIDER);

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repositories;

    @Override
    protected void onFeatureUpdated(Feature feature) {
        addSpiFlyIfAbsent(feature);
    }

    /**
     * Add SpiFly to the dependencies of the given feature if it is not already present.
     */
    private static void addSpiFlyIfAbsent(Feature feature) {
        if (!containsSpiFly(feature)) {
            addSpiFly(feature);
        }
    }

    /**
     * Add SpiFly to the dependencies of the given feature.
     */
    private static void addSpiFly(Feature feature) {
        Dependency dependency = new Dependency("spifly", null);
        dependency.setPrerequisite(true);
        feature.getFeature().add(dependency);
    }

    /**
     * Check if the given feature contains SpiFly as part of its dependencies.
     * @return {@code true} if the feature contains SpiFly, {@code false} otherwise.
     */
    private static boolean containsSpiFly(Feature feature) {
        return feature.getFeature().stream().anyMatch(d -> "spifly".equals(d.getName()));
    }

    @Override
    protected boolean processWrappedBundle(Bundle bundle) {
        String location = bundle.getLocation();
        Matcher matcher = WRAP_PROTOCOL.matcher(location);
        if (matcher.matches()) {
            String groupId = matcher.group(1);
            String artifactId = matcher.group(2);
            String version = matcher.group(3);
            String options = matcher.group(4);
            if (options != null && options.contains(SPI_PROVIDER)) {
                return false;
            } else if (provideSPI(groupId, artifactId, version)) {
                addHeader(bundle, options);
                return true;
            }
        }
        return false;
    }

    /**
     * Add the SPI-Provider header to the given bundle's location.
     */
    private static void addHeader(Bundle bundle, String options) {
        String separator;
        if (options == null) {
            separator = "$";
        } else if (options.endsWith("&") || options.endsWith("$")) {
            separator = "";
        } else {
            separator = "&";
        }
        bundle.setLocation("%s%s%s".formatted(bundle.getLocation(), separator, SPI_HEADER));
    }

    /**
     * Check if the given artifact corresponding to the given maven coordinates provides at least
     * one implementation of service.
     */
    private boolean provideSPI(String groupId, String artifactId, String version) {
        File file = resolveArtifact(groupId, artifactId, version);
        if (file == null) {
            getLog().warn("Could not download artifact %s:%s:%s".formatted(groupId, artifactId, version));
            return false;
        }
        try {
            return containsSPI(file);
        } catch (IOException e) {
            getLog().warn("Could not check artifact %s:%s:%s".formatted(groupId, artifactId, version), e);
        }
        return false;
    }

    /**
     * Check if the given archive contains at least one implementation of a service.
     */
    private static boolean containsSPI(File archive) throws IOException {
        try (ZipFile zip = new ZipFile(archive)) {
            return zip.getEntry("META-INF/services") != null;
        }
    }

    /**
     * Resolve the artifact corresponding to the given maven coordinates from the repositories.
     * @return the resolved file or null if the artifact could not be resolved.
     */
    private File resolveArtifact(String groupId, String artifactId, String version) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Resolving artifact %s:%s:%s".formatted(groupId, artifactId, version));
        }
        ArtifactRequest req = new ArtifactRequest()
                .setRepositories(this.repositories)
                .setArtifact(new DefaultArtifact(groupId, artifactId, "jar", version));
        try {
            return this.repoSystem.resolveArtifact(this.repoSession, req).getArtifact().getFile();
        } catch (Exception e) {
            getLog().warn("Artifact %s could not be resolved.".formatted(artifactId), e);
        }
        return null;
    }
}
