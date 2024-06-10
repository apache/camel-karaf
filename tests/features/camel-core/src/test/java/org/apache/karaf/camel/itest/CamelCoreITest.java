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

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelRouteWithBundleITest;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultFileBasedRoute;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRoute;
import org.apache.karaf.camel.itests.CamelContextProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CamelCoreITest extends AbstractCamelRouteWithBundleITest {

    @Override
    protected String getTestBundleName() {
        return "camel-core-test";
    }

    @Override
    protected List<String> getRequiredFeatures() {
        //camel-blueprint is not strictly required for the RouteSuppliers, but since the bundle contains some blueprints in
        // OSGI-INF, the feature must be installed to avoid a class resolution error.
        return List.of("camel-blueprint");
    }

    @Override
    protected void stopContext() {
        //do nothing because the same supplier context is used across the different tests in camel-core
    }

    @Test
    public void testCamelFile() throws Exception {
        new CamelFileITest(this, getBaseDir()).testRoutes();
    }

    @Test
    public void testCamelSeda() throws Exception {
        new CamelSedaITest(this).testRoutes();
    }

    @Test
    public void testCamelTimer() throws Exception {
        new CamelTimerITest(this).testRoutes();
    }

    @Test
    public void testCamelBean() throws Exception {
        new CamelBeanITest(this).testRoutes();
    }

    public static class CamelFileITest extends AbstractCamelSingleFeatureResultFileBasedRoute {

        public CamelFileITest(CamelContextProvider provider, String baseDir) {
            super(provider, baseDir);
        }

        @Override
        protected void executeTest() throws Exception {
            assertResultFileContains("OK");
        }
    }

    public static class CamelSedaITest extends AbstractCamelSingleFeatureResultMockBasedRoute {

        public CamelSedaITest(CamelContextProvider provider) {
            super(provider);
        }

        @Override
        public void configureMock(MockEndpoint mock) {
            mock.expectedBodiesReceived("OK");
        }
    }

    public static class CamelTimerITest extends AbstractCamelSingleFeatureResultMockBasedRoute {

        public CamelTimerITest(CamelContextProvider provider) {
            super(provider);
        }

        @Override
        public void configureMock(MockEndpoint mock) {
            mock.expectedBodiesReceived("OK");
        }
    }

    public static class CamelBeanITest extends AbstractCamelSingleFeatureResultMockBasedRoute {

        public CamelBeanITest(CamelContextProvider provider) {
            super(provider);
        }

        @Override
        public void configureMock(MockEndpoint mock) {
            mock.expectedBodiesReceived("OK");
        }
    }
}