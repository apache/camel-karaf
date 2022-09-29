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
package org.apache.camel.catalog.karaf;

import java.util.List;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KarafRuntimeProviderTest {

    static CamelCatalog catalog;

    @BeforeClass
    public static void createCamelCatalog() {
        catalog = new DefaultCamelCatalog();
        catalog.setRuntimeProvider(new KarafRuntimeProvider());
    }

    @Test
    public void testGetVersion() throws Exception {
        String version = catalog.getCatalogVersion();
        assertNotNull(version);

        String loaded = catalog.getLoadedVersion();
        assertNotNull(loaded);
        assertEquals(version, loaded);
    }

    @Test
    public void testProviderName() throws Exception {
        assertEquals("karaf", catalog.getRuntimeProvider().getProviderName());
    }

    @Test
    public void testFindComponentNames() throws Exception {
        List<String> names = catalog.findComponentNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        // core components
        assertTrue(names.contains("direct"));
        assertTrue(names.contains("seda"));
        assertTrue(names.contains("vm"));
        assertTrue(names.contains("log"));
        assertTrue(names.contains("mock"));
        assertTrue(names.contains("bean"));

        // regular components
        assertTrue(names.contains("ftp"));
        assertTrue(names.contains("http"));
        assertTrue(names.contains("jetty"));
        assertTrue(names.contains("zookeeper"));

        // hbase is not in karaf
        assertFalse(names.contains("hbase"));

        // pax is from camel-karaf
        assertTrue(names.contains("paxlogging"));
    }

    @Test
    public void testFindDataFormatNames() throws Exception {
        List<String> names = catalog.findDataFormatNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        assertTrue(names.contains("bindyCsv"));
        assertTrue(names.contains("zipDeflater"));
        assertTrue(names.contains("zipFile"));
    }

    @Test
    public void testFindLanguageNames() throws Exception {
        List<String> names = catalog.findLanguageNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        assertTrue(names.contains("bean"));
        assertTrue(names.contains("simple"));
        assertTrue(names.contains("spel"));
        assertTrue(names.contains("xpath"));
    }

    @Test
    public void testFindOtherNames() throws Exception {
        List<String> names = catalog.findOtherNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        // regular others
        assertTrue(names.contains("swagger-java"));
        assertTrue(names.contains("zipkin"));

        // camel-karaf only
        assertTrue(names.contains("blueprint"));

        assertFalse(names.contains("spring-boot"));
    }

}
