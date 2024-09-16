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

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelRouteWithBundleITest;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRoute;
import org.apache.karaf.camel.itests.CamelContextProvider;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.assertNotNull;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@CamelKarafTestHint(isBlueprintTest = true)
public class CamelDisruptorTest extends AbstractCamelRouteWithBundleITest {

    @Override
    protected String getTestBundleName() {
        return "camel-disruptor-test";
    }

    @Override
    protected List<String> getRequiredFeatures() {
        return List.of("camel-disruptor");
    }

    @Test
    public void testCamelDisruptor() throws Exception {
        new CamelDisruptorITest(this).testRoutes();
    }

    @Test
    public void testCamelDisruptorVm() throws Exception {
        new CamelDisruptorVmITest(this).testRoutes();
    }

    public static class CamelDisruptorITest extends AbstractCamelSingleFeatureResultMockBasedRoute {

        public CamelDisruptorITest(CamelContextProvider provider) {
            super(provider);
        }

        @Override
        public void configureMock(MockEndpoint mock) {
            mock.expectedBodiesReceived("OK");
        }

        @Override
        public CamelContext getContext() {
            return getProvider().getDefaultContext();
        }

        @Override
        public ProducerTemplate getTemplate() {
            return getProvider().getDefaultTemplate();
        }
    }

    public static class CamelDisruptorVmITest extends AbstractCamelSingleFeatureResultMockBasedRoute {

        public CamelDisruptorVmITest(CamelContextProvider provider) {
            super(provider);
        }

        @Override
        public void configureMock(MockEndpoint mock) {
            mock.expectedBodiesReceived("OK");
        }

        @Override
        public MockEndpoint getMockEndpoint() {
            return getConsumerContext().getEndpoint("mock:camel-disruptor-vm-test", MockEndpoint.class);
        }

        @Override
        public CamelContext getContext() {
            return getConsumerContext();
        }

        @Override
        public void triggerProducerRoute() {
            Endpoint directEndpoint = getSupplierContext().hasEndpoint("direct:camel-disruptor-vm-test");
            assertNotNull(directEndpoint);
            getProvider().getTemplate("ctx1").send(directEndpoint, getProcessorToCallOnSend());
        }

        private CamelContext getSupplierContext() {
            return getProvider().getContext("ctx1");
        }

        private CamelContext getConsumerContext() {
            return getProvider().getContext("ctx2");
        }
    }
}