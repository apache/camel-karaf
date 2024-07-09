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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.apache.karaf.camel.itests.TemporaryFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@CamelKarafTestHint(externalResourceProvider = CamelBarcodeITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelBarcodeITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    public static final String IMAGE_PROPERTY = "barcode.image";

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
        assertTrue(Files.size(Path.of(System.getProperty(IMAGE_PROPERTY))) > 0);
    }

    public static final class ExternalResourceProviders {
        public static TemporaryFile createTempFile() throws IOException {
            return new TemporaryFile(IMAGE_PROPERTY,"barcode","png");
        }
    }
}