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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link MultiModuleWrapperHandler}.
 */
class MultiModuleWrapperHandlerTest {

    @Test
    void testAll() throws Exception {
        Path camelKarafComponentRoot = Paths.get("target/test-classes/multi-module-wrapper/camel-karaf");
        MultiModuleWrapperHandler handler = new MultiModuleWrapperHandler(
                camelKarafComponentRoot,
                Paths.get("target/test-classes/multi-module-wrapper/camel"),
                "4.0.0");
        handler.add("camel-b", "camel-b2");
        Path pathRootPom = Paths.get("target/test-classes/multi-module-wrapper/camel-karaf/pom.xml");
        assertEquals(
                Files.readString(Paths.get("target/test-classes/multi-module-wrapper/camel-karaf/expected-pom.xml")),
                Files.readString(pathRootPom));
        handler.add("camel-b", "camel-b1");
        handler.add("camel-b", "camel-b3");
        Path pathPomB = camelKarafComponentRoot.resolve("camel-b/pom.xml");
        assertTrue(Files.exists(pathPomB));
        assertEquals(
                Files.readString(Paths.get("target/test-classes/multi-module-wrapper/camel-karaf/b-pom-expected.xml")),
                Files.readString(pathPomB));
        Path pathPom = camelKarafComponentRoot.resolve("camel-b/camel-b1/pom.xml");
        assertTrue(Files.exists(pathPom));
        assertEquals(
                Files.readString(Paths.get("target/test-classes/multi-module-wrapper/camel-karaf/b1-pom-expected.xml")),
                Files.readString(pathPom));
        assertTrue(Files.exists(camelKarafComponentRoot.resolve("camel-b/camel-b2/pom.xml")));
        assertTrue(Files.exists(camelKarafComponentRoot.resolve("camel-b/camel-b3/pom.xml")));
        handler.remove("camel-b", "camel-b1");
        assertFalse(Files.exists(camelKarafComponentRoot.resolve("camel-b/camel-b1")));
        handler.remove("camel-b", "camel-b2");
        assertFalse(Files.exists(camelKarafComponentRoot.resolve("camel-b/camel-b2")));
        assertEquals(
                Files.readString(Paths.get("target/test-classes/multi-module-wrapper/camel-karaf/b-pom-after-remove-expected.xml")),
                Files.readString(pathPomB));
        handler.remove("camel-b", "camel-b3");
        assertFalse(Files.exists(camelKarafComponentRoot.resolve("camel-b")));
        assertEquals(
                Files.readString(Paths.get("src/test/resources/multi-module-wrapper/camel-karaf/pom.xml")),
                Files.readString(pathRootPom));
    }
}
