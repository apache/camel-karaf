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
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link PomUpdater}.
 */
class PomUpdaterTest {

    @Test
    void testAll() throws Exception {
        PomUpdater updater = new PomUpdater(Paths.get("target/test-classes/pom-updater"), "4.1.0");
        updater.execute();
        for (String path : List.of("pom.xml", "level2/pom.xml", "level2/level3/pom.xml")) {
            assertEquals(
                    Files.readString(Paths.get("target/test-classes/pom-updater/pom-expected.xml")),
                    Files.readString(Paths.get("target/test-classes/pom-updater/%s".formatted(path))),
                    "File %s doesn't match with the expected content".formatted(path));
        }
        assertEquals(
                Files.readString(Paths.get("src/test/resources/pom-updater/src/pom.xml")),
                Files.readString(Paths.get("target/test-classes/pom-updater/src/pom.xml")));
    }
}
