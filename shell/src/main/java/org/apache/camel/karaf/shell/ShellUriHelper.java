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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.camel.util.URISupport;

/**
 * Helper to prepare endpoint uris for display by the Camel shell commands.
 */
public final class ShellUriHelper {

    private ShellUriHelper() {
    }

    /**
     * Prepares an endpoint uri so it can be safely written to the console.
     * <p/>
     * The uri is optionally decoded so it is human readable, any credentials are masked, and control characters are
     * left in (or put back into) their percent encoded form. Endpoint uris are not always authored in the route: the
     * dynamic EIPs materialize endpoints whose uri can embed message content, so a decoded uri may carry characters
     * the terminal would otherwise act on.
     *
     * @param  uri    the endpoint uri, may be <tt>null</tt>
     * @param  decode whether to url decode the uri so it is more human readable
     * @return        the uri to display, or <tt>null</tt> if the given uri was <tt>null</tt>
     */
    public static String prepareUriForDisplay(String uri, boolean decode) {
        if (uri == null) {
            return null;
        }
        if (decode) {
            // decode uri so its more human readable
            uri = decodeQuietly(uri);
        }
        // sanitize and mask uri so we don't see passwords
        uri = URISupport.sanitizeUri(uri);
        // must be done last so nothing can put a control character back afterwards
        return encodeControlCharacters(uri);
    }

    /**
     * Url decodes the uri, returning it unchanged when it is not decodable, so that a single malformed escape does not
     * fail the whole command.
     */
    private static String decodeQuietly(String uri) {
        try {
            return URLDecoder.decode(uri, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return uri;
        }
    }

    /**
     * Puts any control character back into its percent encoded form. A uri has no need for control characters, and
     * decoding them would let them reach the terminal, where they can move the cursor, forge additional rows in the
     * output, or be interpreted as an escape sequence.
     */
    private static String encodeControlCharacters(String uri) {
        int first = indexOfControlCharacter(uri, 0);
        if (first == -1) {
            // by far the common case, so do not build anything
            return uri;
        }
        StringBuilder sb = new StringBuilder(uri.length() + 16);
        sb.append(uri, 0, first);
        for (int i = first; i < uri.length(); i++) {
            char ch = uri.charAt(i);
            if (Character.isISOControl(ch)) {
                sb.append('%').append(String.format("%02X", (int) ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static int indexOfControlCharacter(String uri, int from) {
        for (int i = from; i < uri.length(); i++) {
            if (Character.isISOControl(uri.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
