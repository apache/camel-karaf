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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

Path actual = Paths.get(basedir.getAbsolutePath(), "target/feature/result.xml");

if (!Files.exists(actual)) {
    throw new FileNotFoundException("Could not find generated file: $actual");
}

Path expected = Paths.get(basedir.getAbsolutePath(), "src/main/feature/expected.xml");

if (!Files.exists(actual)) {
    throw new FileNotFoundException("Could not find expected file: $expected");
}

String actualContent = Files.readString(actual);
String expectedContent = Files.readString(expected);

if (actualContent != expectedContent) {
    throw new Exception("Expected and actual features files are not equal");
}

Path log = Paths.get(basedir.getAbsolutePath(), "build.log");

if (!Files.exists(log)) {
    throw new FileNotFoundException("Could not find the log file: $log");
}

String logContent = Files.readString(log);

if (!logContent.contains("[ERROR] No root bundles found")) {
    throw new Exception("Could not find the expected error message in the log file for the feature with-placeholders-but-no-root");
}

if (!logContent.contains("[ERROR] Version of the artifact com.networknt.foo/json-schema-validator could not be auto-detected")
    || !logContent.contains("[ERROR] Version of the artifact org.yaml/snakeyaml-foo could not be auto-detected")) {
    throw new Exception("Could not find the expected error message in the log file for the feature with-placeholders-on-non-existing-dependencies");
}

return true;