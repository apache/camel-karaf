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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@CamelKarafTestHint
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CamelAsn1ITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        try {
            final byte[] content = Files.readAllBytes(
                    Path.of("%s/test-classes/asn1_data/SMS_SINGLE.tt".formatted(System.getProperty("project.target"))));
            mock.expectedMessageCount(1);
            mock.expectedMessagesMatches(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    byte[] body = exchange.getIn().getBody(byte[].class);
                    return Arrays.equals(content, body);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

}