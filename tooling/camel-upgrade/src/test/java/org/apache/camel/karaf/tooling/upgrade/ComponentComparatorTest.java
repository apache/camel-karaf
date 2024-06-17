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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link ComponentComparator}.
 */
class ComponentComparatorTest {

    @Test
    void testAll() throws IOException {
        InnerComponentComparator comparator = new InnerComponentComparator();
        comparator.execute();
        assertFalse(comparator.hasAddedComponent("camel-both-single"));
        assertFalse(comparator.hasRemovedComponent("camel-both-single"));
        assertFalse(comparator.hasAddedComponent("camel-both-multiple"));
        assertFalse(comparator.hasRemovedComponent("camel-both-multiple"));
        assertTrue(comparator.hasAddedComponentWithoutSubComponents("camel-only"));
        assertTrue(comparator.hasRemovedComponentWithoutSubComponents("camel-karaf-only"));
        assertTrue(comparator.hasAddedSubComponent("camel-multiple", "camel-multiple-only"));
        assertTrue(comparator.hasRemovedSubComponent("camel-karaf-multiple", "camel-karaf-multiple-only"));
        assertTrue(comparator.hasAddedComponentWithoutSubComponents("camel-complex-single"));
        assertTrue(comparator.hasAddedSubComponent("camel-complex-multiple", "camel-complex-multiple-a"));
        assertTrue(comparator.hasAddedSubComponent("camel-complex-multiple", "camel-complex-multiple-b"));
        assertTrue(comparator.hasAddedSubComponent("camel-complex-multiple", "camel-complex-multiple"));
    }

    private static class InnerComponentComparator extends ComponentComparator {

        private final Map<String, Set<String>> addedComponents = new HashMap<>();
        private final Map<String, Set<String>> removedComponents = new HashMap<>();

        InnerComponentComparator() {
            super(Paths.get("src/test/resources/comparator/camel-karaf"), Paths.get("src/test/resources/comparator/camel"));
        }

        @Override
        protected void onAddSubComponent(String parent, String subComponent) {
            addedComponents.computeIfAbsent(parent, k -> new HashSet<>()).add(subComponent);
        }

        @Override
        protected void onAddComponent(String component) {
            addedComponents.put(component, Set.of());
        }

        @Override
        protected void onRemoveSubComponent(String parent, String subComponent) {
            removedComponents.computeIfAbsent(parent, k -> new HashSet<>()).add(subComponent);
        }

        @Override
        protected void onRemoveComponent(String component) {
            removedComponents.put(component, Set.of());
        }

        boolean hasAddedComponent(String component) {
            return addedComponents.containsKey(component);
        }

        boolean hasRemovedComponent(String component) {
            return removedComponents.containsKey(component);
        }

        boolean hasAddedComponentWithoutSubComponents(String component) {
            return hasAddedComponent(component) && addedComponents.get(component).isEmpty();
        }

        boolean hasRemovedComponentWithoutSubComponents(String component) {
            return hasRemovedComponent(component) && removedComponents.get(component).isEmpty();
        }

        boolean hasAddedSubComponent(String parent, String subComponent) {
            return addedComponents.getOrDefault(parent, Set.of()).contains(subComponent);
        }

        boolean hasRemovedSubComponent(String parent, String subComponent) {
            return removedComponents.getOrDefault(parent, Set.of()).contains(subComponent);
        }
    }
}
