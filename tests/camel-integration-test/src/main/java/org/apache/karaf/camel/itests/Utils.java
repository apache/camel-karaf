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
package org.apache.karaf.camel.itests;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public final class Utils {

    private Utils() {
    }

    static String toKebabCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }

    public static int getAvailablePort(int min, int max) {
        for (int port = min; port <= max; port++) {
            try (ServerSocket socket = new ServerSocket()) {
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
                return socket.getLocalPort();
            } catch (Exception e) {
                System.err.println("Port " + port + " not available, trying next one");
            }
        }
        throw new IllegalStateException("Can't find available network ports");
    }

    public static int getNextAvailablePort() {
        return getAvailablePort(30000, 40000);
    }

    /**
     * Get the Camel context name from the given class.
     *
     * @param clazz the class from which the Camel context name should be extracted
     * @return the Camel context name if it can be found, {@code null} otherwise.
     */
    public static String getCamelContextName(Class<?> clazz) {
        CamelKarafTestHint hint = clazz.getAnnotation(CamelKarafTestHint.class);
        if (hint == null || hint.camelContextName().isEmpty()) {
            return null;
        }
        return hint.camelContextName();
    }
}
