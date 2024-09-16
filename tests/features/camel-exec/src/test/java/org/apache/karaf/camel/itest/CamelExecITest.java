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

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.component.exec.ExecResult;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CamelExecITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedMessageCount(1);
        mock.expectedMessagesMatches(new Predicate() {

            @Override
            public boolean matches(Exchange exchange) {
                ExecResult execResult = exchange.getIn().getBody(ExecResult.class);
                try {
                    String out = new String(execResult.getStderr().readAllBytes());
                    return out.contains("Server VM") && execResult.getExitValue() == 0;
                } catch (IOException e) {
                    return false;
                }
            }
        });
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }
}