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
 * Unit test for {@link FeatureHandler}.
 */
class FeatureHandlerTest {

    @DisplayName("Should fail when the file doesn't contain the starting pattern")
    @Test
    void fileEmpty() {
        FeatureHandler handler = new FeatureHandler(Paths.get("target/test-classes/features/empty.xml"));
        assertThrows(IllegalStateException.class, () -> handler.add("camel-foo"));
    }

    @DisplayName("Should be able to inject a feature in first position")
    @Test
    void injectBefore() throws Exception {
        FeatureHandler handler = new FeatureHandler(Paths.get("target/test-classes/features/inject-before.xml"));
        handler.add("camel-a");
        assertEquals(
            Files.readString(Paths.get("target/test-classes/features/inject-before-expected.xml")),
            Files.readString(Paths.get("target/test-classes/features/inject-before.xml")));
    }

    @DisplayName("Should be able to inject a feature between existing features")
    @Test
    void injectMiddle() throws Exception {
        FeatureHandler handler = new FeatureHandler(Paths.get("target/test-classes/features/inject-middle.xml"));
        handler.add("camel-c");
        assertEquals(
            Files.readString(Paths.get("target/test-classes/features/inject-middle-expected.xml")),
            Files.readString(Paths.get("target/test-classes/features/inject-middle.xml")));
    }

    @DisplayName("Should be able to inject a feature in last position")
    @Test
    void injectAfter() throws Exception {
        FeatureHandler handler = new FeatureHandler(Paths.get("target/test-classes/features/inject-after.xml"));
        handler.add("camel-e");
        assertEquals(
            Files.readString(Paths.get("target/test-classes/features/inject-after-expected.xml")),
            Files.readString(Paths.get("target/test-classes/features/inject-after.xml")));
    }

    @DisplayName("Should be able to remove a feature in first position")
    @Test
    void removeBefore() throws Exception {
        FeatureHandler handler = new FeatureHandler(Paths.get("target/test-classes/features/remove-before.xml"));
        handler.remove("camel-a");
        assertEquals(
            Files.readString(Paths.get("target/test-classes/features/remove-before-expected.xml")),
            Files.readString(Paths.get("target/test-classes/features/remove-before.xml")));
    }

    @DisplayName("Should be able to remove a feature between existing features")
    @Test
    void removeMiddle() throws Exception {
        FeatureHandler handler = new FeatureHandler(Paths.get("target/test-classes/features/remove-middle.xml"));
        handler.remove("camel-b");
        assertEquals(
            Files.readString(Paths.get("target/test-classes/features/remove-middle-expected.xml")),
            Files.readString(Paths.get("target/test-classes/features/remove-middle.xml")));
    }

    @DisplayName("Should be able to remove a feature in last position")
    @Test
    void removeAfter() throws Exception {
        FeatureHandler handler = new FeatureHandler(Paths.get("target/test-classes/features/remove-after.xml"));
        handler.remove("camel-c");
        assertEquals(
            Files.readString(Paths.get("target/test-classes/features/remove-after-expected.xml")),
            Files.readString(Paths.get("target/test-classes/features/remove-after.xml")));
    }
}
