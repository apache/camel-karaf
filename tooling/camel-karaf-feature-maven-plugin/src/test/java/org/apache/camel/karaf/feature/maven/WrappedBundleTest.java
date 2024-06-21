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

import org.apache.karaf.features.internal.model.Bundle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The unit test class for {@link WrappedBundle}.
 */
class WrappedBundleTest {

    private final Bundle bundle = new Bundle();

    @Test
    void fromBundleTest() {
        bundle.setLocation("mvn:commons-io/commons-io/2.15.1$Bundle-Version=2.15.0");
        WrappedBundle wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNull(wrappedBundle);
    }

    @Test
    void getGroupIdTest() {
        bundle.setLocation("wrap:mvn:com.google.oauth-client/google-oauth-client-jetty/${google-oauth-client-version}$overwrite=merge&Import-Package=com.sun.net.httpserver;resolution:=optional,*");
        WrappedBundle wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("com.google.oauth-client", wrappedBundle.getGroupId());

        bundle.setLocation("wrap:mvn:org.kie/kie-api/8.44.0.Final");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("org.kie", wrappedBundle.getGroupId());

        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Export-Package=org.apache.olingo.*;version=5.0.0");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("org.apache.olingo", wrappedBundle.getGroupId());

        bundle.setLocation("wrap:mvn:io.grpc/grpc-core/${grpc-version}$${spi-provider}");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("io.grpc", wrappedBundle.getGroupId());

        bundle.setLocation("wrap:mvn:io.grpc/grpc-googleapis/1.63.0$SPI-Provider=*");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("io.grpc", wrappedBundle.getGroupId());
    }

    @Test
    void getArtifactIdTest() {
        bundle.setLocation("wrap:mvn:com.google.oauth-client/google-oauth-client-jetty/${google-oauth-client-version}$overwrite=merge&Import-Package=com.sun.net.httpserver;resolution:=optional,*");
        WrappedBundle wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("google-oauth-client-jetty", wrappedBundle.getArtifactId());

        bundle.setLocation("wrap:mvn:org.kie/kie-api/8.44.0.Final");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("kie-api", wrappedBundle.getArtifactId());

        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Export-Package=org.apache.olingo.*;version=5.0.0");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("odata-server-core", wrappedBundle.getArtifactId());

        bundle.setLocation("wrap:mvn:io.grpc/grpc-core/${grpc-version}$${spi-provider}");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("grpc-core", wrappedBundle.getArtifactId());

        bundle.setLocation("wrap:mvn:io.grpc/grpc-googleapis/1.63.0$SPI-Provider=*");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("grpc-googleapis", wrappedBundle.getArtifactId());
    }

    @Test
    void getVersionTest() {
        bundle.setLocation("wrap:mvn:com.google.oauth-client/google-oauth-client-jetty/${google-oauth-client-version}$overwrite=merge&Import-Package=com.sun.net.httpserver;resolution:=optional,*");
        WrappedBundle wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("${google-oauth-client-version}", wrappedBundle.getVersion());

        bundle.setLocation("wrap:mvn:org.kie/kie-api/8.44.0.Final");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("8.44.0.Final", wrappedBundle.getVersion());

        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Export-Package=org.apache.olingo.*;version=5.0.0");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("5.0.0", wrappedBundle.getVersion());

        bundle.setLocation("wrap:mvn:io.grpc/grpc-core/${grpc-version}$${spi-provider}");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("${grpc-version}", wrappedBundle.getVersion());

        bundle.setLocation("wrap:mvn:io.grpc/grpc-googleapis/1.63.0$SPI-Provider=*");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("1.63.0", wrappedBundle.getVersion());
    }

    @Test
    void getInstructionsTest() {
        bundle.setLocation("wrap:mvn:com.google.oauth-client/google-oauth-client-jetty/${google-oauth-client-version}$overwrite=merge&Import-Package=com.sun.net.httpserver;resolution:=optional,*");
        WrappedBundle wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("$overwrite=merge&Import-Package=com.sun.net.httpserver;resolution:=optional,*", wrappedBundle.getInstructions());

        bundle.setLocation("wrap:mvn:org.kie/kie-api/8.44.0.Final");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertNull(wrappedBundle.getInstructions());

        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Export-Package=org.apache.olingo.*;version=5.0.0");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("$overwrite=merge&Export-Package=org.apache.olingo.*;version=5.0.0", wrappedBundle.getInstructions());

        bundle.setLocation("wrap:mvn:io.grpc/grpc-core/${grpc-version}$${spi-provider}");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("$${spi-provider}", wrappedBundle.getInstructions());

        bundle.setLocation("wrap:mvn:io.grpc/grpc-googleapis/1.63.0$SPI-Provider=*");
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals("$SPI-Provider=*", wrappedBundle.getInstructions());
    }
}
