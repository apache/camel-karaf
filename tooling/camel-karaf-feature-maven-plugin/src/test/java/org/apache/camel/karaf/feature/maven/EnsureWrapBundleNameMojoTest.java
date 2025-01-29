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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.karaf.features.internal.model.Bundle;
import org.junit.jupiter.api.Test;

public class EnsureWrapBundleNameMojoTest {

    private final EnsureWrapBundleNameMojo ensureNameMojo = new EnsureWrapBundleNameMojo();

    @Test
    void modifyLocationTest() throws Exception {
        Bundle bundle = new Bundle();
        // add bundle name at the end as first wrap protocol option
        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0");
        String expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-Name=Wrap%20of%20Odata%20Server%20Core&Bundle-SymbolicName=wrap_org.apache.olingo.odata-server-core";
        WrappedBundle wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals(expected, ensureNameMojo.processLocation(wrappedBundle));

        // add bundle name at the end but not as first wrap protocol option
        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge");
        expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Bundle-Name=Wrap%20of%20Odata%20Server%20Core&Bundle-SymbolicName=wrap_org.apache.olingo.odata-server-core";
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals(expected, ensureNameMojo.processLocation(wrappedBundle));

        // add bundle name before existing wrap protocol header that should be declared after
        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Export-Package=org.apache.olingo.*;version=5.0.0");
        expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Bundle-Name=Wrap%20of%20Odata%20Server%20Core&Bundle-SymbolicName=wrap_org.apache.olingo.odata-server-core&Export-Package=org.apache.olingo.*;version=5.0.0";
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals(expected, ensureNameMojo.processLocation(wrappedBundle));

        // add bundle name before existing wrap protocol header that should be declared after
        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Bundle-Version=5.0.0&Export-Package=org.apache.olingo.*;version=5.0.0");
        expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$overwrite=merge&Bundle-Name=Wrap%20of%20Odata%20Server%20Core&Bundle-SymbolicName=wrap_org.apache.olingo.odata-server-core&Bundle-Version=5.0.0&Export-Package=org.apache.olingo.*;version=5.0.0";
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals(expected, ensureNameMojo.processLocation(wrappedBundle));

        // don't override the Bundle-Name if present
        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-Name=MyName");
        expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-Name=MyName&Bundle-SymbolicName=wrap_org.apache.olingo.odata-server-core";
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals(expected, ensureNameMojo.processLocation(wrappedBundle));

        // don't override the Bundle-SymbolicName if present
        bundle.setLocation("wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-SymbolicName=MyName");
        expected = "wrap:mvn:org.apache.olingo/odata-server-core/5.0.0$Bundle-SymbolicName=MyName&Bundle-Name=Wrap%20of%20Odata%20Server%20Core";
        wrappedBundle = WrappedBundle.fromBundle(bundle);
        assertNotNull(wrappedBundle);
        assertEquals(expected, ensureNameMojo.processLocation(wrappedBundle));

    }


}