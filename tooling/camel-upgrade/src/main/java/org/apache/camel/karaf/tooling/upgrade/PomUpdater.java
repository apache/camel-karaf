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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.regex.Pattern;

import static org.apache.camel.karaf.tooling.upgrade.Utils.replaceFileContent;

public class PomUpdater extends SimpleFileVisitor<Path> {

    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of("target", ".git", "src", ".mvn");
    private static final Pattern VERSION_TAG_PATTERN = Pattern.compile("<version>[^<]+-SNAPSHOT</version>");
    private final String camelVersion;
    private final Path camelKarafRootPom;

    public PomUpdater(Path camelKarafRootPom, String camelVersion) {
        this.camelVersion = camelVersion;
        this.camelKarafRootPom = camelKarafRootPom;
    }

    public void execute() throws IOException {
        Files.walkFileTree(camelKarafRootPom, this);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (acceptDirectory(dir)) {
            return FileVisitResult.CONTINUE;
        }
        return FileVisitResult.SKIP_SUBTREE;
    }

    private boolean acceptDirectory(Path dir) {
        return !EXCLUDED_DIRECTORIES.contains(dir.getFileName().toString());
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (isPomFile(file)) {
            updatePom(file);
        }
        return FileVisitResult.CONTINUE;
    }

    private void updatePom(Path file) throws IOException {
        replaceFileContent(file, content -> VERSION_TAG_PATTERN.matcher(content).replaceAll("<version>%s-SNAPSHOT</version>".formatted(camelVersion)));
    }

    private boolean isPomFile(Path file) {
        return file.getFileName().toString().equals("pom.xml");
    }
}
