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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * Verifies the {@code camel-tika} feature installs and a {@code tika:parse} route runs end-to-end
 * (issue #713). The point of the test is that the feature now resolves: before the fix the
 * {@code tika-parser-text-module} bundle failed to wire because its {@code juniversalchardet}
 * dependency (package {@code org.mozilla.universalchardet}) was missing from the feature, so the
 * route could never be created and no message would reach the mock.
 * <p>
 * The test deliberately does not assert on the extracted text: Tika's {@code AutoDetectParser}
 * discovers parsers through the JDK {@link java.util.ServiceLoader}, which does not cross OSGi
 * bundle boundaries, so {@code tika:parse} yields empty content in Karaf. Wiring Tika's parser SPI
 * for OSGi is a separate concern beyond the scope of issue #713.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CamelTikaITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    private static final String TEXT_SAMPLE = "The quick brown fox jumps over the lazy dog";

    @Override
    public String getBodyToSend() {
        return TEXT_SAMPLE;
    }

    @Override
    public void configureMock(MockEndpoint mock) {
        // The feature resolves and the route processes exactly one exchange without error.
        mock.expectedMessageCount(1);
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }
}
