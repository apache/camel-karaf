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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link DefaultValuesProvider}.
 */
class DefaultValuesProviderTest {

    @DisplayName("Should not fail when the file is absent")
    @Test
    void fileAbsent() {
        DefaultValuesProvider provider = new DefaultValuesProvider("nonexistent");
        assertTrue(provider.getDefaultCamelVersion().isEmpty());
        assertTrue(provider.getDefaultCamelKarafRoot().isEmpty());
        assertTrue(provider.getDefaultCamelRoot().isEmpty());
    }

    @DisplayName("Should not fail when the file is empty")
    @Test
    void fileEmpty() {
        DefaultValuesProvider provider = new DefaultValuesProvider("src/test/resources/default-values/empty.properties");
        assertTrue(provider.getDefaultCamelVersion().isEmpty());
        assertTrue(provider.getDefaultCamelKarafRoot().isEmpty());
        assertTrue(provider.getDefaultCamelRoot().isEmpty());
    }

    @DisplayName("Should find values when available")
    @Test
    void fileFull() {
        DefaultValuesProvider provider = new DefaultValuesProvider("src/test/resources/default-values/full.properties");
        assertTrue(provider.getDefaultCamelVersion().isPresent());
        assertEquals("4.0.0", provider.getDefaultCamelVersion().get());
        assertTrue(provider.getDefaultCamelKarafRoot().isPresent());
        assertEquals("camel-karaf-root", provider.getDefaultCamelKarafRoot().get());
        assertTrue(provider.getDefaultCamelRoot().isPresent());
        assertEquals("camel-root", provider.getDefaultCamelRoot().get());
    }
}
