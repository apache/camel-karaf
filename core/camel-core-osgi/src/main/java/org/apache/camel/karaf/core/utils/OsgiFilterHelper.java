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

/**
 * Helper to build OSGi service filters from values that are not known to be filter safe.
 */
public final class OsgiFilterHelper {

    private OsgiFilterHelper() {
    }

    /**
     * Builds the filter <tt>(key=value)</tt>, escaping the value so it is matched literally.
     *
     * @param  key   the attribute to match on, must be a literal known to the caller
     * @param  value the value to match, escaped before being embedded
     * @return       the filter expression
     */
    public static String createFilter(String key, String value) {
        return "(" + key + "=" + escapeFilterValue(value) + ")";
    }

    /**
     * Escapes the characters that are significant in an OSGi filter value.
     * <p/>
     * The OSGi core specification defines its own filter grammar, in which a value escapes <tt>(</tt>, <tt>)</tt>,
     * <tt>*</tt> and <tt>\</tt> by prefixing a single backslash. Note this is not the <tt>\2a</tt> hex form used by
     * the LDAP string representation in RFC 4515: an OSGi {@code Filter} would read that as the two literal
     * characters <tt>2a</tt>.
     * <p/>
     * Without escaping, a name is parsed as filter syntax rather than matched as text, so <tt>*</tt> becomes a
     * presence assertion matching every registered service, and an unbalanced parenthesis makes the framework reject
     * the filter instead of simply not matching.
     *
     * @param  value the value to escape, may be <tt>null</tt>
     * @return       the escaped value, or <tt>null</tt> if the given value was <tt>null</tt>
     */
    public static String escapeFilterValue(String value) {
        if (value == null) {
            return null;
        }
        int first = indexOfSignificantCharacter(value);
        if (first == -1) {
            // legitimate component, language, dataformat and bean names never need escaping
            return value;
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        sb.append(value, 0, first);
        for (int i = first; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '*', '(', ')', '\\' -> sb.append('\\').append(ch);
                default -> sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static int indexOfSignificantCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            switch (value.charAt(i)) {
                case '*', '(', ')', '\\' -> {
                    return i;
                }
                default -> {
                    // keep looking
                }
            }
        }
        return -1;
    }
}
