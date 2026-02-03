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


import org.apache.karaf.features.internal.model.Bundle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

public class EnsureWrapBundleVersionMojoTest {

    private final EnsureWrapBundleVersionMojo ensureVersionMojo = new EnsureWrapBundleVersionMojo();

    @Test
    void modifyLocationTest() throws Exception {
        Bundle bundle = new Bundle();
        // add bundle version at the end as first wrap protocol option
        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0");
        String expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-Version=5.0.0";
        WrappedBundle wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals(expected, ensureVersionMojo.processLocation(wrappedBundle));

        // add bundle version at the end but not as first wrap protocol option
        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge");
        expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Bundle-Version=5.0.0";
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals(expected, ensureVersionMojo.processLocation(wrappedBundle));

        // add bundle version before existing wrap protocol header that should be declared after
        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Export-Package=org.apache.olingo.*;version=5.0.0");
        expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Bundle-Version=5.0.0&Export-Package=org.apache.olingo.*;version=5.0.0";
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals(expected, ensureVersionMojo.processLocation(wrappedBundle));

        // original version won't work in Karaf
        bundle.setLocation("wrap:mvn:com.google.apis/google-api-services-storage/v1-rev20240209-2.0.0");
        expected = "wrap:mvn:com.google.apis/google-api-services-storage/v1-rev20240209-2.0.0$Bundle-Version=0.0.0.v1-rev20240209-2_0_0";
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals(expected, ensureVersionMojo.processLocation(wrappedBundle));

        // bundle version header is present but it won't work in Karaf
        bundle.setLocation("wrap:mvn:com.google.apis/google-api-services-storage/v1-rev20240209-2.0.0$Bundle-Version=v1-rev20240209-2.0.0");
        expected = "wrap:mvn:com.google.apis/google-api-services-storage/v1-rev20240209-2.0.0$Bundle-Version=0.0.0.v1-rev20240209-2_0_0";
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals(expected, ensureVersionMojo.processLocation(wrappedBundle));
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
        int versionHeaderStartIndex = location.indexOf(BUNDLE_VERSION);
        assertEquals(108, ensureVersionMojo.getBundleVersionHeaderEndIndex(location, versionHeaderStartIndex));

        location = "wrap:mvn:org.apache.qpid/qpid-jms-client/${qpid-jms-client-version}$Bundle-Version=${qpid-jms-client-version}";
        versionHeaderStartIndex = location.indexOf(BUNDLE_VERSION);
        assertEquals(108, ensureVersionMojo.getBundleVersionHeaderEndIndex(location, versionHeaderStartIndex));
    }
}