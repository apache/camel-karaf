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
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CamelFlatpackITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    private static final String TO_PARSE = """
            "ITEM_DESC","IN_STOCK","PRICE","LAST_RECV_DT"
            "SOME VALVE","2","5.00","20050101"
            "AN ENGINE","100","1000.00","20040601"
            "A BELT","45",".50","20030101"
            "A BOLT","1000","2.75","20050101"
            """;

    private static final String[] EXPECTED_ITEM_DESCRIPTIONS = { "SOME VALVE", "AN ENGINE", "A BELT", "A BOLT" };

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedMessageCount(4);
        List<Exchange> list = mock.getReceivedExchanges();
        int counter = 0;
        for (Exchange exchange : list) {
            Message in = exchange.getIn();
            Map<?, ?> body = in.getBody(Map.class);
            assertNotNull(body);
            assertEquals(EXPECTED_ITEM_DESCRIPTIONS[counter], "ITEM_DESC", body.get("ITEM_DESC"));
            counter++;
        }
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    @Override
    public String getBodyToSend() {
        return TO_PARSE;
    }

}