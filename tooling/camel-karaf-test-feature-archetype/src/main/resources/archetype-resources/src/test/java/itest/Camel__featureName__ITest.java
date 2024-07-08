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

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@CamelKarafTestHint(externalResourceProvider = Camel${featureName}ITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class Camel${featureName}ITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {
        /* TODO : add external resource
        public static AvailablePortProvider createAvailablePortProvider() {
            return new AvailablePortProvider(List.of("${featureNameLower}.port"));
        }*/
    }
}