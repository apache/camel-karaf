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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {

    private static final Pattern LINE_SEPARATOR_PATTERN = Pattern.compile("\r\n|[\n\r\u2028\u2029\u0085]");
    private Utils() {}

    static String loadFileFromClassLoader(String path) {
        try (InputStream resource = UpgradeCamel.class.getResourceAsStream(path)) {
            if (resource == null) {
                throw new IOException("Resource %s not found".formatted(path));
            }
            return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void replaceFileContent(Path path, UnaryOperator<String> contentUpdater) throws IOException {
        Files.writeString(path, contentUpdater.apply(Files.readString(path, StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    static int nextLineIndex(String content, int start) {
        Matcher matcher = LINE_SEPARATOR_PATTERN.matcher(content);
        if (matcher.find(start)) {
            return matcher.end();
        }
        return -1;
    }
}
