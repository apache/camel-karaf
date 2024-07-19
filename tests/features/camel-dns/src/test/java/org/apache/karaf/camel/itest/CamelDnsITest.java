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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CamelDnsITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedMessageCount(3);
    }

    @Test
    public void testResultMock() throws Exception {
        MockEndpoint endpoint = getMockEndpoint();
        List<Exchange> exchanges = endpoint.getExchanges();
        assertNotNull(exchanges);
        assertEquals(3, exchanges.size());

        String ip = exchanges.get(0).getIn().getBody(String.class);
        assertNotNull(ip);

        Record[] lookupAnswers = exchanges.get(1).getIn().getBody(Record[].class);
        assertTrue(lookupAnswers.length > 0);
        assertNotNull((lookupAnswers[0].getName()));

        Message digResult = exchanges.get(2).getIn().getBody(Message.class);
        assertNotNull(digResult.getQuestion());
        assertNotNull(digResult.getSection(Section.ANSWER));

        assertMockEndpointsSatisfied();
    }
}