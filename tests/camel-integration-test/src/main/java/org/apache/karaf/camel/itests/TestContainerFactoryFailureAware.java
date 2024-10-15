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
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerFactory;
import org.ops4j.pax.exam.spi.PaxExamRuntime;
import org.ops4j.pax.exam.util.PathUtils;

import static org.apache.karaf.camel.itests.AbstractCamelRouteITest.CAMEL_KARAF_INTEGRATION_TEST_DUMP_LOGS_PROPERTY;

public class TestContainerFactoryFailureAware implements TestContainerFactory {

    private final boolean enabled;
    private final TestContainerFactory delegate;

    public TestContainerFactoryFailureAware() {
        this.enabled = Boolean.getBoolean(CAMEL_KARAF_INTEGRATION_TEST_DUMP_LOGS_PROPERTY);
        this.delegate = PaxExamRuntime.getTestContainerFactory();
    }

    @Override
    public TestContainer[] create(ExamSystem system) {
        TestContainer[] containers = delegate.create(system);
        if (enabled) {
            for (int i = 0; i < containers.length; i++) {
                containers[i] = new TestContainerFailureAware(containers[i]);
            }
        }
        return containers;
    }

    private static class TestContainerFailureAware implements TestContainer {

        private final TestContainer delegate;

        private TestContainerFailureAware(TestContainer delegate) {
            this.delegate = delegate;
        }

        @Override
        public TestContainer start() {
            try {
                return delegate.start();
            } catch (RuntimeException e) {
                Utils.dumpFile(getKarafLogFile(), System.err);
                throw e;
            }
        }

        @Override
        public long install(InputStream stream) {
            return delegate.install(stream);
        }

        @Override
        public long install(String location, InputStream stream) {
            return delegate.install(location, stream);
        }

        @Override
        public long installProbe(InputStream stream) {
            return delegate.installProbe(stream);
        }

        @Override
        public void uninstallProbe() {
            delegate.uninstallProbe();
        }

        @Override
        public void call(TestAddress address) {
            delegate.call(address);
        }

        @Override
        public TestContainer stop() {
            return delegate.stop();
        }

        private static File getKarafUnpackDirectory() {
            return new File(PathUtils.getBaseDir(), "target/exam");
        }

        private static File getKarafLogFile() {
            return new File(searchKarafBase(getKarafUnpackDirectory()), "data/log/karaf.log");
        }

        /**
         * Since we might get quite deep use a simple breath first search algorithm
         */
        private static File searchKarafBase(File root) {
            Queue<File> searchNext = new LinkedList<>();
            searchNext.add(root);
            while (!searchNext.isEmpty()) {
                File head = searchNext.poll();
                if (!head.isDirectory()) {
                    continue;
                }
                if (new File(head, "system").isDirectory() && new File(head, "etc").isDirectory()) {
                    return head;
                }
                searchNext.addAll(List.of(Objects.requireNonNull(head.listFiles())));
            }
            return null;
        }
    }
}
