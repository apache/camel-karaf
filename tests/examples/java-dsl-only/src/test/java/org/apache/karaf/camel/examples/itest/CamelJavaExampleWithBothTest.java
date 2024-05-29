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

import java.util.List;

import org.apache.camel.Endpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelRouteWithBundleITest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.assertNotNull;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CamelJavaExampleWithBothTest extends AbstractCamelRouteWithBundleITest {

    @Override
    protected List<String> getRequiredFeatures() {
        return List.of();
    }

    @Override
    protected String getTestBundleName() {
        return "camel-karaf-examples-java-dsl-only-test";
    }

    @Test
    public void testJavaDSL1() throws Exception {
        verify(1);
    }


    @Test
    public void testJavaDSL2() throws Exception {
        verify(2);
    }

    private void verify(int id) throws InterruptedException {
        Endpoint endpoint = getContext().hasEndpoint("direct:example%d".formatted(id));
        assertNotNull(endpoint);
        MockEndpoint mock = getContext().getEndpoint("mock:example%d".formatted(id), MockEndpoint.class);
        mock.expectedBodiesReceived("Hello World");
        getTemplate().sendBody("direct:example%d".formatted(id), "Hello World");
        mock.assertIsSatisfied();
    }
}