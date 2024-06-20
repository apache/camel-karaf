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
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for {@link ParentPomUpdater}.
 */
class ParentPomUpdaterTest {

    @DisplayName("Should fail when the Camel's pom file doesn't contain the starting pattern")
    @Test
    void noStartInCamel() {
        ParentPomUpdater updater = new ParentPomUpdater(
                Paths.get("target/test-classes/parent-pom-updater/camel-karaf-pom-no-start.xml"),
                Paths.get("target/test-classes/parent-pom-updater/camel-pom-no-start.xml"),
                "4.1.0");
        assertThrows(IllegalStateException.class, updater::execute);
    }

    @DisplayName("Should fail when the Camel Karaf's pom file doesn't contain the starting pattern")
    @Test
    void noStartInCamelKaraf() {
        ParentPomUpdater updater = new ParentPomUpdater(
                Paths.get("target/test-classes/parent-pom-updater/camel-karaf-pom-no-start.xml"),
                Paths.get("target/test-classes/parent-pom-updater/camel-pom.xml"),
                "4.1.0");
        assertThrows(IllegalStateException.class, updater::execute);
    }

    @DisplayName("Should fail when the Camel Karaf's pom file doesn't contain the ending pattern")
    @Test
    void noEndInCamelKaraf() {
        ParentPomUpdater updater = new ParentPomUpdater(
                Paths.get("target/test-classes/parent-pom-updater/camel-karaf-pom-no-end.xml"),
                Paths.get("target/test-classes/parent-pom-updater/camel-pom.xml"),
                "4.1.0");
        assertThrows(IllegalStateException.class, updater::execute);
    }

    @DisplayName("Should properly update the Camel Karaf's pom file as expected")
    @Test
    void updatePom() throws Exception {
        ParentPomUpdater updater = new ParentPomUpdater(
                Paths.get("target/test-classes/parent-pom-updater/camel-karaf-pom.xml"),
                Paths.get("target/test-classes/parent-pom-updater/camel-pom.xml"),
                "4.1.0");
        updater.execute();
        assertEquals(
                Files.readString(Paths.get("target/test-classes/parent-pom-updater/camel-karaf-pom-expected.xml")),
                Files.readString(Paths.get("target/test-classes/parent-pom-updater/camel-karaf-pom.xml")));
    }

}
