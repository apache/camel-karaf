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
package org.apache.karaf.camel.examples.itest;

import org.apache.camel.Endpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@CamelKarafTestHint(isBlueprintTest = true)
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CamelVmITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

   public String getCamelFeatureName() {
       return "camel-core";
   }

   public void setupMock() {
        MockEndpoint endpoint = getContext("ctx2").getEndpoint("mock:%s".formatted(getTestComponentName()), MockEndpoint.class);
        endpoint.setFailFast(false);
        configureMock(endpoint);
        Endpoint directEndpoint = getContext("ctx1").hasEndpoint("direct:%s".formatted(getTestComponentName()));
        if (directEndpoint != null) {
           getTemplate().send(directEndpoint, getProcessorToCallOnSend());
        }
   }

   @Test
   public void testDirectVM() throws Exception {
       MockEndpoint.assertIsSatisfied(getContext("ctx2"));
   }
}