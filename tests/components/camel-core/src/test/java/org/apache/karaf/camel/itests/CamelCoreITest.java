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
package org.apache.karaf.camel.itests;

import java.util.List;

import org.apache.karaf.camel.test.CamelFileITest;
import org.apache.karaf.camel.test.CamelSedaITest;
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
        return List.of();
    }

    @Test
    public void testCamelFile() throws Exception {
        new CamelFileITest(context, template, getBaseDir()).testRoutes();
    }

    @Test
    public void testCamelSeda() throws Exception {
        new CamelSedaITest(context, template).testRoutes();
    }
}