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

package org.apache.karaf.camel.itest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.apache.karaf.camel.itests.ExternalResource;

/**
 * A resource that manages the lifecycle of the MongoDB container for testing purposes. The integration test has many
 * limitations, such as the requirement to use the MongoDB port which could cause conflicts with concurrent builds.
 * For some unknown reason, if the container is managed by test containers, the connection to the database fails, reason
 * why this class has been added as workaround.
 */
class MongoContainerResource implements ExternalResource {

    private static final String CONTAINER_NAME = "mongo";
    private static final String DEBEZIUM_VERSION = "2.7";
    private static final String MONGO_DB_IMAGE = "quay.io/debezium/example-mongodb";
    private static final String AUTHENTICATION_DB_NAME = "admin";
    private static final String SOURCE_DB_NAME = "inventory";
    private static final String SOURCE_COLLECTION_NAME = "products";
    private static final String SOURCE_DB_USERNAME = "debezium";
    private static final String SOURCE_DB_PASSWORD = "dbz";
    private static final int MONGODB_PORT = 27017;

    @Override
    public void before() {
        startContainer();
        intContainer();
    }

    private static void startContainer() {
        try {
            Command command = new Command(
                "docker", "run", "--rm", "--name", CONTAINER_NAME, "-p", "%d:%d".formatted(MONGODB_PORT, MONGODB_PORT),
                "-e", "MONGODB_USER=%s".formatted(SOURCE_DB_USERNAME), "-e", "MONGODB_PASSWORD=%s".formatted(SOURCE_DB_PASSWORD),
                "%s:%s".formatted(MONGO_DB_IMAGE, DEBEZIUM_VERSION)
            );
            command.waitForResult("(?i).*waiting for connections.*");
        } catch (Exception e) {
            throw new RuntimeException("Error starting MongoDB container", e);
        }
    }

    private static void intContainer() {
        try {
            Command command = new Command("docker", "exec", CONTAINER_NAME, "bash", "-c", "/usr/local/bin/init-inventory.sh -h localhost");
            command.waitForResult();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing MongoDB container", e);
        }
    }

    private static void stopContainer() {
        try {
            Command command = new Command("docker", "stop", CONTAINER_NAME);
            command.waitForResult();
        } catch (Exception e) {
            throw new RuntimeException("Error stopping MongoDB container", e);
        }
    }

    @Override
    public void after() {
        stopContainer();
    }

    @Override
    public Map<String, String> properties() {
        return Map.of(
            "mongo.connection", "mongodb://localhost:%d/?replicaSet=rs0".formatted(MONGODB_PORT),
            "mongo.collection", SOURCE_COLLECTION_NAME,
            "mongo.authentication.database", AUTHENTICATION_DB_NAME,
            "mongo.database", SOURCE_DB_NAME,
            "mongo.username", SOURCE_DB_USERNAME,
            "mongo.password", SOURCE_DB_PASSWORD
        );
    }

    private static class Command {
        private static final Pattern ERRORS_TO_IGNORE = Pattern.compile(
                "(?i).*unable to find image.*|.*pulling.*|.*waiting.*|.*verifying checksum.*|" +
                        ".*(download|pull) complete.*|.*digest:.*|.*downloaded newer image for.*"
        );
        private final Process process;

        Command(String... command) throws IOException {
            this.process = new ProcessBuilder(command).start();
        }

        void waitForResult() {
            waitForResult(
                CompletableFuture.runAsync(this::checkExitCode), CompletableFuture.runAsync(this::checkError)
            );
        }

        void waitForResult(String startupPattern) {
            waitForResult(
                CompletableFuture.supplyAsync(this::checkExitCode),
                CompletableFuture.supplyAsync(() -> checkStartupPattern(startupPattern)),
                CompletableFuture.supplyAsync(this::checkError)
            );
        }

        private void waitForResult(CompletableFuture<?>... cfs) {
            ProcessResult result = (ProcessResult) CompletableFuture.anyOf(cfs)
                             .join();
            if (result != null && !result.isSuccess()) {
                throw new RuntimeException(result.getError());
            }
        }

        private ProcessResult checkExitCode() {
            try {
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return new ProcessResult(true);
                }
                return new ProcessResult("Process exited with code %d".formatted(exitCode));
            } catch (InterruptedException e) {
                return new ProcessResult("The thread was interrupted");
            } catch (RuntimeException e) {
                return new ProcessResult("Could not check the exit code: %s".formatted(e.getMessage()));
            }
        }

        private ProcessResult checkStartupPattern(String startupPattern) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                Pattern pattern = Pattern.compile(startupPattern);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (pattern.matcher(line).matches()) {
                        return new ProcessResult(true);
                    }
                }
            } catch (Exception e) {
                return new ProcessResult("Could not check the status pattern: %s".formatted(e.getMessage()));
            }
            return new ProcessResult("The startup pattern %s could not be found".formatted(startupPattern));
        }

        private ProcessResult checkError() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (ERRORS_TO_IGNORE.matcher(line).matches()) {
                        continue;
                    }
                    return new ProcessResult(line);
                }
            } catch (Exception e) {
                return new ProcessResult("Could not check the error messages: %s".formatted(e.getMessage()));
            }
            return new ProcessResult(true);
        }
    }

    private static class ProcessResult {
        private final boolean success;
        private final String error;

        private ProcessResult(boolean success) {
            this.success = success;
            this.error = null;
        }

        private ProcessResult(String error) {
            this.success = false;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return Optional.ofNullable(error).orElse("Process failed");
        }
    }
}
