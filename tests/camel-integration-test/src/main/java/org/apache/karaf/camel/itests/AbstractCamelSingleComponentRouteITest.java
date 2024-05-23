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

import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;

import static org.apache.karaf.camel.itests.Utils.toKebabCase;

public abstract class AbstractCamelSingleComponentRouteITest extends AbstractCamelRouteWithBundleITest {

    public String getTestComponentName() {
        return getTestClassSimpleNameInKebabCase().replace("-itest", "-test");
    }

    public String getCamelFeatureName() {
        return getTestClassSimpleNameInKebabCase().replace("-itest", "");
    }

    protected int getTimeoutInSeconds() {
        return 5;
    }

    @NotNull
    private String getTestClassSimpleNameInKebabCase() {
        String name = toKebabCase(this.getClass().getSimpleName());
        if (!name.endsWith("-itest")) {
            throw new IllegalArgumentException("The integration test class name doesn't match with the expected format: <tested-camel-component-name>ITest");
        }
        return name;
    }

    @Override
    protected String getTestBundleName() {
        return getTestComponentName();
    }

    @Override
    protected List<String> getRequiredFeatures() {
        return List.of(getCamelFeatureName());
    }

    @Before
    public void triggerProducerRoute() {
        Endpoint endpoint = context.hasEndpoint("direct:%s".formatted(getTestComponentName()));
        if (endpoint != null) {
            template.send(endpoint, getProcessorToCallOnSend());
        }
    }

    protected Processor getProcessorToCallOnSend() {
        return exchange -> exchange.getMessage().setBody(getBodyToSend());
    }
    protected String getBodyToSend() {
        return getClass().getSimpleName();
    }
}
