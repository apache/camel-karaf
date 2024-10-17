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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.IntPredicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.ops4j.pax.exam.MavenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private Utils() {
    }

    static String toKebabCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }

    public static int getAvailablePort(int min, int max) {
        return getAvailablePort(min, max, null);
    }

    public static int getAvailablePort(int min, int max, IntPredicate filter) {
        for (int port = min; port <= max; port++) {
            try (ServerSocket socket = new ServerSocket()) {
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
                if (filter == null || filter.test(port)) {
                    return socket.getLocalPort();
                }
            } catch (Exception e) {
                LOG.debug("Port {} not available, trying next one", port);
            }
        }
        throw new IllegalStateException("Can't find available network ports");
    }

    public static int getNextAvailablePort() {
        return getNextAvailablePort(null);
    }

    public static int getNextAvailablePort(IntPredicate filter) {
        return getAvailablePort(30000, 40000, filter);
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

    static String loadCamelKarafVersion() {
        try {
            String version = MavenUtils.asInProject().getVersion("org.apache.camel.karaf", "camel-integration-test");
            LOG.info("Detected Camel Karaf version: {}", version);
            return version;
        } catch (Exception e) {
            LOG.debug("Can't detect Camel Karaf version", e);
        }
        return null;
    }

    static String loadUsersFileIfAbsent(String baseDir) {
        Path location = Paths.get(baseDir, "camel-karaf-itest-resources", "users.properties");
        if (Files.exists(location)) {
            LOG.debug("Detected users file at {}", location);
            return location.toString();
        }
        return loadUsersFile(location);
    }

    @NotNull
    private static String loadUsersFile(Path location) {
        try (InputStream is = Utils.class.getResourceAsStream("/etc/users.properties")) {
            if (is != null) {
                Files.createDirectories(location.getParent());
                try (OutputStream os = Files.newOutputStream(location)) {
                    is.transferTo(os);
                }
                return location.toString();
            }
        } catch (Exception e) {
            LOG.debug("Can't load the users.properties file", e);
        }
        throw new IllegalStateException("Can't find the users.properties file, please provide it using the system " +
                "property users.file.location");
    }

    /**
     * Dump the given file into the stream.
     */
    static void dumpFile(File file, PrintStream out) {
        if (file.exists()) {
            out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            out.printf(">>>>> START Dumping file %s%n", file.getName());
            out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            try (Stream<String> lines = Files.lines(file.toPath())) {
                lines.forEach(out::println);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            out.printf("<<<<< END Dumping file %s%n", file.getName());
            out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        }
    }
}
