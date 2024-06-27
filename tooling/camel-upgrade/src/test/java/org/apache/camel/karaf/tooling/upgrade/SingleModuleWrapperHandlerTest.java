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
 * Unit test for {@link SingleModuleWrapperHandler}.
 */
class SingleModuleWrapperHandlerTest {

    @Test
    void testAll() throws Exception {
        SingleModuleWrapperHandler handler = new SingleModuleWrapperHandler(
                Paths.get("target/test-classes/single-module-wrapper/camel-karaf"),
                Paths.get("target/test-classes/single-module-wrapper/camel"),
                "4.0.0");
        handler.add("camel-a", "camel-a");
        Path pathPom = Paths.get("target/test-classes/single-module-wrapper/camel-karaf/camel-a/pom.xml");
        assertTrue(Files.exists(pathPom));
        assertEquals(
                Files.readString(Paths.get("target/test-classes/single-module-wrapper/camel-karaf/a-pom-expected.xml")),
                Files.readString(pathPom));
        handler.add("camel-c", "camel-c");
        pathPom = Paths.get("target/test-classes/single-module-wrapper/camel-karaf/camel-c/pom.xml");
        assertTrue(Files.exists(pathPom));
        assertEquals(
                Files.readString(Paths.get("target/test-classes/single-module-wrapper/camel-karaf/c-pom-expected.xml")),
                Files.readString(pathPom));
        handler.add("camel-e", "camel-e");
        assertTrue(Files.exists(Paths.get("target/test-classes/single-module-wrapper/camel-karaf/camel-e/pom.xml")));
        handler.add("camel-a/camel-f", "camel-f");
        pathPom = Paths.get("target/test-classes/single-module-wrapper/camel-karaf/camel-f/pom.xml");
        assertTrue(Files.exists(pathPom));
        assertEquals(
                Files.readString(Paths.get("target/test-classes/single-module-wrapper/camel-karaf/f-pom-expected.xml")),
                Files.readString(pathPom));
        assertEquals(
                Files.readString(Paths.get("target/test-classes/single-module-wrapper/camel-karaf/pom-expected.xml")),
                Files.readString(Paths.get("target/test-classes/single-module-wrapper/camel-karaf/pom.xml")));
        handler.remove("camel-f");
        assertFalse(Files.exists(Paths.get("target/test-classes/single-module-wrapper/camel-karaf/camel-f")));
        handler.remove("camel-c");
        assertFalse(Files.exists(Paths.get("target/test-classes/single-module-wrapper/camel-karaf/camel-c")));
        handler.remove("camel-e");
        assertFalse(Files.exists(Paths.get("target/test-classes/single-module-wrapper/camel-karaf/camel-e")));
        handler.remove("camel-a");
        assertFalse(Files.exists(Paths.get("target/test-classes/single-module-wrapper/camel-karaf/camel-a")));
        assertEquals(
                Files.readString(Paths.get("src/test/resources/single-module-wrapper/camel-karaf/pom.xml")),
                Files.readString(Paths.get("target/test-classes/single-module-wrapper/camel-karaf/pom.xml")));
    }
}
