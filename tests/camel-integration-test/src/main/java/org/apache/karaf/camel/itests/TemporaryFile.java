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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A temporary file that is deleted when the test is finished.
 */
public class TemporaryFile implements ExternalResource {
    private static final Logger LOG = LoggerFactory.getLogger(TemporaryFile.class);
    private final String key;
    private final Path path;

    /**
     * Create a temporary file with the given key.
     *
     * @param key the key to store the path of the temporary file
     * @param prefix the prefix of the temporary file
     * @param suffix the suffix of the temporary file
     * @throws IOException if an I/O error occurs
     */
    public TemporaryFile(String key, String prefix, String suffix) throws IOException {
        this.key = key;
        this.path = Files.createTempFile(prefix, suffix);
    }

    @Override
    public void before() {
        // Do nothing
    }

    @Override
    public void after() {
        try {
            if (Files.deleteIfExists(path)) {
                LOG.debug("Deleted temporary file: {}", path);
            }
        } catch (IOException e) {
            LOG.warn("Failed to delete temporary file: {}", path, e);
        }
    }

    @Override
    public Map<String, String> properties() {
        return Map.of(key, path.toString());
    }

    public Path getPath() {
        return path;
    }
}
