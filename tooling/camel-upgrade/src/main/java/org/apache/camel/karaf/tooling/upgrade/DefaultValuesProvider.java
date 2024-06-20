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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultValuesProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultValuesProvider.class);
    private static final String CAMEL_ROOT_KEY = "camel.root";
    private static final String CAMEL_KARAF_ROOT_KEY = "camel.karaf.root";
    private static final String CAMEL_VERSION_KEY = "camel.version";

    private final Properties defaultValues;

    public DefaultValuesProvider(String path) {
        this.defaultValues = loadDefaultValues(path);
    }

    private static Properties loadDefaultValues(String path) {
        LOG.debug("Loading default values from {}", path);
        Properties properties = new Properties();
        File file = new File(path);
        if (file.exists()) {
            LOG.debug("Loading default values from {}", file);
            try (FileReader reader = new FileReader(file)) {
                properties.load(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return properties;
    }

    private Optional<String> getDefaultValue(String key) {
        return Optional.ofNullable(defaultValues.get(key))
                .map(Object::toString)
                .map(String::trim)
                .filter(s -> !s.isEmpty());
    }

    public Optional<String> getDefaultCamelRoot() {
        return getDefaultValue(CAMEL_ROOT_KEY);
    }

    public Optional<String> getDefaultCamelKarafRoot() {
        return getDefaultValue(CAMEL_KARAF_ROOT_KEY);
    }

    public Optional<String> getDefaultCamelVersion() {
        return getDefaultValue(CAMEL_VERSION_KEY);
    }
}
