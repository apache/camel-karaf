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
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.GenericContainerResource;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Consumer;


@CamelKarafTestHint(externalResourceProvider = CamelInfluxdb2ITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelInfluxdb2ITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceivedInAnyOrder("OK_PING", "OK_INSERT");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {
        private static final String ADMIN_TOKEN = "_QGcCkmN0_tzesB9OemUNR7c3WEqk35LV8ymQRhiSy6x9tQIPw0DgWCSpcnC0B_kIEeNG6aLpAoCAV1-lDRzKA==";
        private static final String ORG = "test-org";
        private static final String BUCKET = "test-bucket";

        public static GenericContainerResource createInfluxdb2Container() {

            final GenericContainer<?> influxDBContainer =
                    new InfluxDBContainer<>(DockerImageName.parse("influxdb:2.0.7"))
                    .withAdminToken(ADMIN_TOKEN)
                    .withBucket(BUCKET)
                    .withOrganization(ORG)
                    .withAuthEnabled(true)
                    .withExposedPorts(8086);

            return new GenericContainerResource(influxDBContainer, (Consumer<GenericContainerResource>)
                    resource -> {
                        resource.setProperty("influxdb2.port", Integer.toString(influxDBContainer.getMappedPort(8086)));
                        resource.setProperty("influxdb2.admin.token", ADMIN_TOKEN);
                        resource.setProperty("influxdb2.bucket", BUCKET);
                        resource.setProperty("influxdb2.org", ORG);
                    }
            );
        }
    }
}