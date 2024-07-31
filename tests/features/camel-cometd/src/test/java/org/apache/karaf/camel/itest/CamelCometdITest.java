/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.camel.itest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.apache.karaf.camel.itests.AvailablePortProvider;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@CamelKarafTestHint(
        externalResourceProvider = CamelCometdITest.ExternalResourceProviders.class,
        isBlueprintTest = true
)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelCometdITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("Cometd message received");
    }

    //override to install the test bundle without starting it as it is a Fragment
    @Override
    protected List<String> installRequiredBundles() throws Exception {
        String testBundleName = getTestBundleName();
        String testBundleVersion = getTestBundleVersion();
        if (testBundleVersion == null) {
            throw new IllegalArgumentException("The system property project.version must be set to the version of the " +
                    "test bundle to install or the method getTestBundleVersion must be overridden to provide the version");
        }
        Path bundlePath = Paths.get("%s/%s-%s.jar".formatted(getBaseDir(), testBundleName, testBundleVersion));
        installBundle(bundlePath.toUri().toString(), false);
        //refresh the host bundle to make the test fragment available
        findBundleByName("camel-integration-test").update();
        return List.of(testBundleName);
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {
        public static AvailablePortProvider createAvailablePortProvider() {
            return new AvailablePortProvider(List.of("cometd.port"));
        }
    }
}