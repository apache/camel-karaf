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
package org.apache.camel.karaf.core.utils;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OsgiFilterHelperTest {

    @Test
    public void testNullValue() {
        assertNull(OsgiFilterHelper.escapeFilterValue(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jms", "http", "aws2-s3", "camel-bean", "xpath", "org.apache.camel.MyBean", "a+b"})
    public void testLegitimateNamesAreUnchanged(String name) {
        assertEquals(name, OsgiFilterHelper.escapeFilterValue(name));
        assertEquals("(component=" + name + ")", OsgiFilterHelper.createFilter("component", name));
    }

    @Test
    public void testSignificantCharactersAreEscaped() {
        // the OSGi filter grammar escapes with a backslash before the character, not with the
        // RFC 4515 hex form - an OSGi Filter reads "\\2a" as the two literal characters 2a
        assertEquals("\\*", OsgiFilterHelper.escapeFilterValue("*"));
        assertEquals("\\(", OsgiFilterHelper.escapeFilterValue("("));
        assertEquals("\\)", OsgiFilterHelper.escapeFilterValue(")"));
        assertEquals("\\\\", OsgiFilterHelper.escapeFilterValue("\\"));
        assertEquals("a\\*b", OsgiFilterHelper.escapeFilterValue("a*b"));
    }

    /**
     * The point of the escaping: a wildcard name must stop selecting every registered service.
     */
    @Test
    public void testWildcardNoLongerMatchesAnArbitraryService() throws InvalidSyntaxException {
        Filter filter = FrameworkUtil.createFilter(OsgiFilterHelper.createFilter("name", "*"));
        assertFalse(filter.match(properties("name", "someRegisteredBean")),
                "an escaped * must not match an unrelated service");
        assertFalse(filter.match(properties("name", "anotherBean")),
                "an escaped * must not match an unrelated service");
        assertTrue(filter.match(properties("name", "*")),
                "it must still match a service whose name really is *");
    }

    @Test
    public void testUnescapedWildcardWouldHaveMatched() throws InvalidSyntaxException {
        // documents the behaviour being fixed
        Filter unescaped = FrameworkUtil.createFilter("(name=*)");
        assertTrue(unescaped.match(properties("name", "someRegisteredBean")));
    }

    @Test
    public void testInjectedFilterSyntaxIsNeutralised() throws InvalidSyntaxException {
        String hostile = "x)(objectClass=org.apache.karaf.features.FeaturesService";
        Filter filter = assertDoesNotThrow(() -> FrameworkUtil.createFilter(OsgiFilterHelper.createFilter("name", hostile)),
                "an escaped name must produce a valid filter rather than a syntax error");
        assertFalse(filter.match(properties("name", "someRegisteredBean")));
        assertFalse(filter.match(properties("objectClass", "org.apache.karaf.features.FeaturesService")));
        assertTrue(filter.match(properties("name", hostile)));
    }

    @Test
    public void testUnbalancedParenthesisNoLongerFaultsTheFilter() {
        assertDoesNotThrow(() -> FrameworkUtil.createFilter(OsgiFilterHelper.createFilter("component", ")(")),
                "an unbalanced parenthesis must not make the framework reject the filter");
    }

    private static Dictionary<String, Object> properties(String key, Object value) {
        Dictionary<String, Object> d = new Hashtable<>();
        d.put(key, value);
        return d;
    }
}
