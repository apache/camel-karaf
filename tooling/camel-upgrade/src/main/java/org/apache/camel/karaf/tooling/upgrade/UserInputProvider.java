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

import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserInputProvider {

    private static final Logger LOG = LoggerFactory.getLogger(UserInputProvider.class);

    private final Console console;
    private final DefaultValuesProvider defaultValuesProvider;

    public UserInputProvider(Console console, DefaultValuesProvider defaultValuesProvider) {
        this.console = Objects.requireNonNull(console, "Console is required");
        this.defaultValuesProvider = Objects.requireNonNull(defaultValuesProvider, "DefaultValuesProvider is required");
    }

    public Path readCamelRootDirectory() {
        return checkCamelRoot(
            readLine("Camel Root Directory%s (cloned if not set): ", "Camel Root Directory",
                    defaultValuesProvider.getDefaultCamelRoot(), false)
        );
    }

    public Path readCamelKarafRootDirectory() {
        return checkCamelKarafRoot(
            readLine("Camel Karaf Root Directory%s: ", "Camel Karaf Root Directory",
                    defaultValuesProvider.getDefaultCamelKarafRoot(), true)
        );
    }

    public String readCamelVersion() {
        return readLine("Expected Camel Version%s: ", "Camel Version", defaultValuesProvider.getDefaultCamelVersion(), true);
    }

    public boolean doAction(String message) {
        String answer = console.readLine("%s (y/n): ".formatted(message));
        return "y".equalsIgnoreCase(answer);
    }

    private String readLine(String messageFormat, String name, Optional<String> defaultValue, boolean required) {
        String value = console.readLine(messageFormat.formatted(defaultValue.map(" [%s]"::formatted).orElse("")));
        if (value == null || value.isBlank()) {
            value = defaultValue.orElse(null);
            if (value == null) {
                if (required) {
                    LOG.error("{} is required.", name);
                    System.exit(1);
                }
            } else {
                LOG.info("Using default {} {}", name, value);
            }
        }
        return value;
    }

    private static Path checkCamelRoot(String camelRootDirectory) {
        if (camelRootDirectory == null || camelRootDirectory.isBlank()) {
            return null;
        }
        Path camelRoot = Paths.get(camelRootDirectory);
        if (!Files.exists(camelRoot)) {
            LOG.error("Camel root directory does not exist.");
            System.exit(1);
        } else if (!Files.exists(camelRoot.resolve("pom.xml"))) {
            LOG.error("Camel root directory does not contain any pom file.");
            System.exit(1);
        } else if (!Files.exists(camelRoot.resolve("components/pom.xml"))) {
            LOG.error("Camel root directory does not contain any sub module components.");
            System.exit(1);
        }
        return camelRoot;
    }

    private static Path checkCamelKarafRoot(String camelKarafRootDirectory) {
        if (camelKarafRootDirectory == null || camelKarafRootDirectory.isBlank()) {
            LOG.error("Camel Karaf root directory is required.");
            System.exit(1);
        }
        Path camelKarafRoot = Paths.get(camelKarafRootDirectory);
        if (!Files.exists(camelKarafRoot)) {
            LOG.error("Camel Karaf root directory does not exist.");
            System.exit(1);
        } else if (!Files.exists(camelKarafRoot.resolve("pom.xml"))) {
            LOG.error("Camel Karaf root directory does not contain any pom file.");
            System.exit(1);
        } else if (!Files.exists(camelKarafRoot.resolve("components/pom.xml"))) {
            LOG.error("Camel Karaf root directory does not contain any sub module components.");
            System.exit(1);
        }
        return camelKarafRoot;
    }
}
