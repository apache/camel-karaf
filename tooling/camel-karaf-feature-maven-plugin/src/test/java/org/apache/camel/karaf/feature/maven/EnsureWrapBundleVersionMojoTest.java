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


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EnsureWrapBundleVersionMojoTest {

    private final EnsureWrapBundleVersionMojo ensureVersionMojo = new EnsureWrapBundleVersionMojo();

    @Test
    void modifyLocationTest() throws Exception {
        // add bundle version at the end as first wrap protocol option
        String location = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0";
        String expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-Version=5.0.0";
        assertEquals(expected, ensureVersionMojo.processLocation(location));

        // add bundle version at the end but not as first wrap protocol option
        location = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge";
        expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Bundle-Version=5.0.0";
        assertEquals(expected, ensureVersionMojo.processLocation(location));

        // add bundle version before existing wrap protocol header that should be declared after
        location = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Export-Package=org.apache.olingo.*;version=5.0.0";
        expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Bundle-Version=5.0.0&Export-Package=org.apache.olingo.*;version=5.0.0";
        assertEquals(expected, ensureVersionMojo.processLocation(location));

        // original version won't work in Karaf
        location = "wrap:mvn:com.google.apis/google-api-services-storage/v1-rev20240209-2.0.0";
        expected = "wrap:mvn:com.google.apis/google-api-services-storage/v1-rev20240209-2.0.0$Bundle-Version=0.0.0.v1-rev20240209-2_0_0";
        assertEquals(expected, ensureVersionMojo.processLocation(location));

        // bundle version header is present but it won't work in Karaf
        location = "wrap:mvn:com.google.apis/google-api-services-storage/v1-rev20240209-2.0.0$Bundle-Version=v1-rev20240209-2.0.0";
        expected = "wrap:mvn:com.google.apis/google-api-services-storage/v1-rev20240209-2.0.0$Bundle-Version=0.0.0.v1-rev20240209-2_0_0";
        assertEquals(expected, ensureVersionMojo.processLocation(location));

        // bundle version header is present and points to the wrong value but it will
        // work in Karaf
        location = "mvn:commons-io/commons-io/2.15.1$Bundle-Version=2.15.0";
        expected = "mvn:commons-io/commons-io/2.15.1$Bundle-Version=2.15.0";
        assertEquals(expected, ensureVersionMojo.processLocation(location));
    }

    @Test
    void getVersionStartIndexTest() {
        assertEquals(51,
                ensureVersionMojo.getVersionStartIndex("wrap:mvn:org.apache.httpcomponents.core5/httpcore5/5.2.1"));
        assertEquals(51, ensureVersionMojo.getVersionStartIndex(
                "wrap:mvn:org.eclipse.californium/element-connector/3.11.0$overwrite=merge&Import-Package=net.i2p.crypto.eddsa;resolution:=optional"));
    }

    @Test
    void getVersionEndIndexTest() {
        assertEquals(55,
                ensureVersionMojo.getVersionEndIndex("wrap:mvn:org.apache.httpcomponents.core5/httpcore5/5.2.1"));
        assertEquals(56, ensureVersionMojo.getVersionEndIndex(
                "wrap:mvn:org.eclipse.californium/element-connector/3.11.0$overwrite=merge&Import-Package=net.i2p.crypto.eddsa;resolution:=optional"));
    }

    @Test
    void getVersionTest() {
        assertEquals("${google-oauth-client-version}", ensureVersionMojo.getVersion(
                "wrap:mvn:com.google.oauth-client/google-oauth-client-jetty/${google-oauth-client-version}$overwrite=merge&Import-Package=com.sun.net.httpserver;resolution:=optional,*"));

        assertEquals("8.44.0.Final", ensureVersionMojo.getVersion("wrap:mvn:org.kie/kie-api/8.44.0.Final"));

        assertEquals("5.0.0", ensureVersionMojo.getVersion(
                "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Export-Package=org.apache.olingo.*;version=5.0.0"));

        assertEquals("${grpc-version}",
                ensureVersionMojo.getVersion("wrap:mvn:io.grpc/grpc-core/${grpc-version}$${spi-provider}"));

        assertEquals("1.63.0", ensureVersionMojo.getVersion("wrap:mvn:io.grpc/grpc-googleapis/1.63.0$SPI-Provider=*"));
    }

    @Test
    void getValidVersionTest() throws Exception {
        // raw version is already valid
        assertEquals("1.2.3", ensureVersionMojo.getValidVersion("", "1.2.3"));
        assertEquals("1.0.0", ensureVersionMojo.getValidVersion("", "1.0"));

        // raw version is invalid
        assertEquals("0.0.0.SRU2023-10_1_4", ensureVersionMojo.getValidVersion("", "SRU2023-10.1.4"));
        assertEquals("1.23.1.alpha", ensureVersionMojo.getValidVersion("", "1.23.1-alpha"));
        assertEquals("0.0.0.v3-rev20240123-2_0_0", ensureVersionMojo.getValidVersion("", "v3-rev20240123-2.0.0"));
        assertEquals("1.1.0.4c", ensureVersionMojo.getValidVersion("", "1.1.4c"));
    }

    @Test
    void updateExistingVersionTest() throws Exception {
        String bundleVersionHeader = "Bundle-Version=9.9.9";

        // no update test
        String location = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-Version=4.4.4";
        String expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-Version=4.4.4";
        String result = ensureVersionMojo.updateExistingVersion(location, bundleVersionHeader);
        assertEquals(expected, result);

        location = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-Version=${unresolved-placeholder-version}";
        expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-Version=9.9.9";
        result = ensureVersionMojo.updateExistingVersion(location, bundleVersionHeader);
        assertEquals(expected, result);

        location = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-Version=invalid_4.4.4";
        expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-Version=9.9.9";
        result = ensureVersionMojo.updateExistingVersion(location, bundleVersionHeader);
        assertEquals(expected, result);

        location = "wrap:mvn:org.eclipse.californium/element-connector/${californium-version}$overwrite=merge&Bundle-Version=invalid_4.4.4&Import-Package=net.i2p.crypto.eddsa;resolution:=optional";
        expected = "wrap:mvn:org.eclipse.californium/element-connector/${californium-version}$overwrite=merge&Bundle-Version=9.9.9&Import-Package=net.i2p.crypto.eddsa;resolution:=optional";
        result = ensureVersionMojo.updateExistingVersion(location, bundleVersionHeader);
        assertEquals(expected, result);
    }

    @Test
    void getBundleVersionHeaderEndIndexTest() {
        String location = "wrap:mvn:org.apache.qpid/qpid-jms-client/${qpid-jms-client-version}$Bundle-Version=${qpid-jms-client-version}&Import-Package=net.i2p.crypto.eddsa;resolution:=optional";
        int versionHeaderStartIndex = location.indexOf(EnsureWrapBundleVersionMojo.BUNDLE_VERSION);
        assertEquals(108, ensureVersionMojo.getBundleVersionHeaderEndIndex(location, versionHeaderStartIndex));

        location = "wrap:mvn:org.apache.qpid/qpid-jms-client/${qpid-jms-client-version}$Bundle-Version=${qpid-jms-client-version}";
        versionHeaderStartIndex = location.indexOf(EnsureWrapBundleVersionMojo.BUNDLE_VERSION);
        assertEquals(108, ensureVersionMojo.getBundleVersionHeaderEndIndex(location, versionHeaderStartIndex));
    }
}