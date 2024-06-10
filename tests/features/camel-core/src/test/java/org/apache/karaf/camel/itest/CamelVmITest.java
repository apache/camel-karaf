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

import static org.junit.Assert.assertNotNull;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@CamelKarafTestHint(isBlueprintTest = true, ignoreRouteSuppliers = true)
public class CamelVmITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

   public String getCamelFeatureName() {
       return "camel-core";
   }

    @Override
    public String getTestComponentName() {
        return "camel-core-test";
    }

    public void setupMock() {
        MockEndpoint endpoint = getConsumerContext().getEndpoint("mock:camel-vm-test", MockEndpoint.class);
        assertNotNull(endpoint);
        endpoint.setFailFast(false);
        endpoint.expectedBodiesReceived("OK");
        Endpoint directEndpoint = getSupplierContext().hasEndpoint("direct:camel-vm-test");
        assertNotNull(directEndpoint);
        getTemplate("ctx1").send(directEndpoint, getProcessorToCallOnSend());
   }

   private CamelContext getSupplierContext() {
        return getContext("ctx1");
    }

   private CamelContext getConsumerContext() {
       return getContext("ctx3");
   }

   @Test
   public void testDirectVM() throws Exception {
       MockEndpoint.assertIsSatisfied(getConsumerContext());
   }
}