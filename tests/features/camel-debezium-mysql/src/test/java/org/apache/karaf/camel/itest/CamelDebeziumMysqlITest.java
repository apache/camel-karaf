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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.MySQLContainer.MYSQL_PORT;

@CamelKarafTestHint(externalResourceProvider = CamelDebeziumMysqlITest.ExternalResourceProviders.class,
        additionalRequiredFeatures = "camel-sql")
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelDebeziumMysqlITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceivedInAnyOrder("101", "102", "103", "104", "105", "106", "107", "108", "109", "1");
    }

    @Test
    @Ignore("TODO: Unable to instantiate class class io.debezium.transforms.outbox.EventRouter Does it have a public no-arg constructor?")
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {

        private static final String MYSQL_IMAGE = "quay.io/debezium/example-mysql";
        private static final String SOURCE_DB_NAME = "inventory";
        private static final String SOURCE_DB_TABLE = String.format("%s.products", SOURCE_DB_NAME);
        private static final String SOURCE_DB_USERNAME = "mysqluser";
        private static final String SOURCE_DB_PASSWORD = "dbz";

        public static GenericContainerResource<MYSQLContainer> createMySQLContainer() {

            // TODO force debezium version to avoid root access issue (instead of System.getProperty("debezium.version"))
            MYSQLContainer container = new MYSQLContainer(DockerImageName.parse(MYSQL_IMAGE).withTag("2.7.1.Final")
                    .asCompatibleSubstituteFor("mysql"))
                    .withUsername(SOURCE_DB_USERNAME)
                    .withPassword(SOURCE_DB_PASSWORD);
            return new GenericContainerResource<>(container,
                    resource -> {
                        resource.setProperty("mysql.host", container.getHost());
                        resource.setProperty("mysql.port", Integer.toString(container.getMappedPort(MYSQL_PORT)));
                        resource.setProperty("mysql.table", SOURCE_DB_TABLE);
                        resource.setProperty("mysql.database", SOURCE_DB_NAME);
                        // Need root access to avoid permission issues
                        resource.setProperty("mysql.username", "root");
                        resource.setProperty("mysql.password", container.getPassword());
                    }
            );
        }
    }

    private static class MYSQLContainer extends MySQLContainer<MYSQLContainer> {
        public MYSQLContainer(DockerImageName dockerImageName) {
            super(dockerImageName);
        }
    }
}