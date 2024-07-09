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
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import com.apptasticsoftware.rssreader.Item;

@CamelKarafTestHint
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CamelAtomITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedMessageCount(7);
        List<Exchange> list = mock.getReceivedExchanges();
        String[] expectedTitles = {
                "Speaking at the Irish Java Technology Conference on Thursday and Friday",
                "a great presentation on REST, JAX-WS and JSR 311",
                "my slides on ActiveMQ and Camel from last weeks Dublin Conference",
                "webcast today on Apache ActiveMQ",
                "Feedback on my Camel talk at the IJTC conference",
                "More thoughts on RESTful Message Queues",
                "ActiveMQ webinar archive available" };
        int counter = 0;
        for (Exchange exchange : list) {
            Item entry = exchange.getIn().getBody(Item.class);
            assertNotNull( "No entry found for exchange: " + exchange,entry);

            String expectedTitle = expectedTitles[counter];
            String title = entry.getTitle().get();
            assertEquals(expectedTitle, title, "Title of message " + counter);

            counter++;
        }
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

}