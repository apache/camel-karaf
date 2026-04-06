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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT;

@CamelKarafTestHint(externalResourceProvider = CamelDebeziumPostgresITest.ExternalResourceProviders.class,
        additionalRequiredFeatures = "camel-sql")
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelDebeziumPostgresITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("101", "102", "103", "104", "105", "106", "107", "108", "109", "1");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {

        private static final String SOURCE_DB_SCHEMA = "inventory";
        private static final String SOURCE_DB_TABLE = String.format("%s.products", SOURCE_DB_SCHEMA);
        private static final String DEBEZIUM_VERSION = "2.7";
        private static final String PGSQL_IMAGE = "quay.io/debezium/example-postgres";
        private static final String SOURCE_DB_NAME = "debezium-db";
        private static final String SOURCE_DB_USERNAME = "debezium";
        private static final String SOURCE_DB_PASSWORD = "dbz";

        public static GenericContainerResource<PGSQLContainer> createPGSQLContainer() {

            PGSQLContainer container = new PGSQLContainer(DockerImageName.parse(PGSQL_IMAGE).withTag(DEBEZIUM_VERSION)
                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName(SOURCE_DB_NAME)
                    .withUsername(SOURCE_DB_USERNAME)
                    .withPassword(SOURCE_DB_PASSWORD);
            return new GenericContainerResource<>(container,
                    resource -> {
                        resource.setProperty("pgsql.host", container.getHost());
                        resource.setProperty("pgsql.port", Integer.toString(container.getMappedPort(POSTGRESQL_PORT)));
                        resource.setProperty("pgsql.schema", SOURCE_DB_SCHEMA);
                        resource.setProperty("pgsql.table", SOURCE_DB_TABLE);
                        resource.setProperty("pgsql.database", container.getDatabaseName());
                        resource.setProperty("pgsql.username", container.getUsername());
                        resource.setProperty("pgsql.password", container.getPassword());
                    }
            );
        }
    }

    private static class PGSQLContainer extends PostgreSQLContainer<PGSQLContainer> {
        public PGSQLContainer(DockerImageName dockerImageName) {
            super(dockerImageName);
        }
    }
}