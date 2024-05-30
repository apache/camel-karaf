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

import java.util.List;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.OptionUtils.combine;

public abstract class AbstractCamelRouteWithBundleITest extends AbstractCamelRouteITest {


    protected abstract String getTestBundleName();

    public String getTestBundleVersion() {
        return System.getProperty("project.version");
    }

    @Override
    protected List<String> installRequiredBundles() throws Exception {
        String testBundleName = getTestBundleName();
        String testBundleVersion = getTestBundleVersion();
        if (testBundleVersion == null) {
            throw new IllegalArgumentException("The system property project.version must be set to the version of the " +
                    "test bundle to install or the method getTestBundleVersion must be overridden to provide the version");
        }
        installBundle("file://%s/%s-%s.jar".formatted(getBaseDir(), testBundleName, testBundleVersion), true);
        assertBundleInstalledAndRunning(testBundleName);
        return List.of(testBundleName);
    }

    @Override
    protected Option[] getAdditionalOptions() {
        return combine(
            super.getAdditionalOptions(), CoreOptions.systemProperty("project.version").value(getTestBundleVersion())
        );
    }
}
