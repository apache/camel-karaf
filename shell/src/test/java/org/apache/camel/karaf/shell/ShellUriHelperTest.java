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
package org.apache.camel.karaf.shell;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShellUriHelperTest {

    private static final char ESC = 0x1B;

    @Test
    public void testNullUri() {
        assertNull(ShellUriHelper.prepareUriForDisplay(null, true));
        assertNull(ShellUriHelper.prepareUriForDisplay(null, false));
    }

    @Test
    public void testPlainUriIsUnchanged() {
        String uri = "http://localhost:8080/push";
        assertEquals(uri, ShellUriHelper.prepareUriForDisplay(uri, true));
        assertEquals(uri, ShellUriHelper.prepareUriForDisplay(uri, false));
    }

    @Test
    public void testDecodeStillMakesUriReadable() {
        assertEquals("timer://foo?period=5000&message=hello world",
                ShellUriHelper.prepareUriForDisplay("timer://foo?period=5000&message=hello%20world", true));
    }

    @Test
    public void testNoDecodeLeavesUriEncoded() {
        assertEquals("timer://foo?message=hello%20world",
                ShellUriHelper.prepareUriForDisplay("timer://foo?message=hello%20world", false));
    }

    @Test
    public void testCredentialsAreStillMasked() {
        String uri = ShellUriHelper.prepareUriForDisplay("ftp://host/dir?username=scott&password=tiger&binary=true", true);
        assertFalse(uri.contains("tiger"), "password must be masked, was: " + uri);
        assertFalse(uri.contains("scott"), "username must be masked, was: " + uri);
        assertTrue(uri.contains("binary=true"), "non credential options are kept, was: " + uri);
        assertTrue(uri.startsWith("ftp://host/dir?"), "the endpoint itself is still readable, was: " + uri);
    }

    @ParameterizedTest
    @ValueSource(strings = {"%1b", "%1B", "%0a", "%0d", "%07", "%00", "%7f"})
    public void testEncodedControlCharacterIsNotReArmedByDecode(String encoded) {
        String uri = ShellUriHelper.prepareUriForDisplay("http://host/" + encoded + "evil", true);
        assertFalse(containsControlCharacter(uri), "decoded uri must not contain a control character, was: " + uri);
    }

    @Test
    public void testEscapeSequenceIsRenderedInline() {
        // %1b decodes to ESC, which must come back as %1B rather than reaching the terminal
        assertEquals("http://host/%1B]2;title%07",
                ShellUriHelper.prepareUriForDisplay("http://host/%1b]2;title%07", true));
    }

    @Test
    public void testNewlineCannotForgeAnExtraRow() {
        String uri = ShellUriHelper.prepareUriForDisplay("http://evil/%0aseda://looks-legit", true);
        assertFalse(uri.contains("\n"), "a decoded newline must not split the row, was: " + uri);
        assertEquals("http://evil/%0Aseda://looks-legit", uri);
    }

    @Test
    public void testLiteralControlCharacterIsEncodedEvenWithoutDecode() {
        String uri = ShellUriHelper.prepareUriForDisplay("http://host/" + ESC + "[2J", false);
        assertFalse(containsControlCharacter(uri), "was: " + uri);
        assertEquals("http://host/%1B[2J", uri);
    }

    @Test
    public void testMalformedEscapeDoesNotFailTheCommand() {
        // an incomplete % sequence makes URLDecoder throw, which used to abort the whole listing
        assertEquals("http://host/100%", ShellUriHelper.prepareUriForDisplay("http://host/100%", true));
        assertEquals("http://host/%zz", ShellUriHelper.prepareUriForDisplay("http://host/%zz", true));
    }

    private static boolean containsControlCharacter(String s) {
        return s.chars().anyMatch(Character::isISOControl);
    }
}
