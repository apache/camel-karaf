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

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CamelBindyITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    public static final String CSV = "1,B2,Keira,Knightley,ISIN,XX23456789,BUY,Share,400.25,EUR,14-01-2009,17-02-2011 23:21:59\r\n";

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived(CSV);
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }
}