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
import org.apache.karaf.camel.test.CamelJaxbRouteSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@CamelKarafTestHint
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CamelJaxbITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    private static final String XML_SAMPLE = "<MyData><name>%s</name><age>%d</age></MyData>"
            .formatted(CamelJaxbRouteSupplier.XML_SAMPLE_NAME, CamelJaxbRouteSupplier.XML_SAMPLE_AGE);

    @Override
    public String getBodyToSend() {
        return XML_SAMPLE;
    }

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived(XML_SAMPLE);
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }
}