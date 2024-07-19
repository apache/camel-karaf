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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CamelCsvITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    private static final String ORIGINAL_CSV_SAMPLE = """
            Jack Dalton, 115, mad at Averell
            Joe Dalton, 105, calming Joe
            William Dalton, 105, keeping Joe from killing Averell
            Averell Dalton, 80, playing with Rantanplan
            Lucky Luke, 120, capturing the Daltons
            """;

    @Override
    public String getBodyToSend() {
        return ORIGINAL_CSV_SAMPLE;
    }

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedMessageCount(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResultMock() throws Exception {
        MockEndpoint endpoint = getMockEndpoint();
        List<Exchange> exchanges = endpoint.getExchanges();
        assertNotNull(exchanges);
        assertEquals(2, exchanges.size());

        List<List<String>> unmarshalResult = (List<List<String>>) exchanges.get(0).getIn().getBody();
        assertNotNull(unmarshalResult);
        assertEquals(5, unmarshalResult.size());
        assertEquals(3, unmarshalResult.get(0).size());
        assertEquals("Jack Dalton", unmarshalResult.get(0).get(0));

        String marshalResult = exchanges.get(1).getIn().getBody(String.class);
        assertEquals(ORIGINAL_CSV_SAMPLE, marshalResult);

        assertMockEndpointsSatisfied();
    }
}