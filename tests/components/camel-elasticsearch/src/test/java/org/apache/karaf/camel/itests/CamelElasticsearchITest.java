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

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

@UseExternalResourceProvider(CamelElasticsearchITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelElasticsearchITest extends AbstractCamelKarafResultMockBasedITest {

    @Override
    protected void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {

        private static final String USER_NAME = "elastic";
        private static final String PASSWORD = "s3cret";
        private static final int ELASTIC_SEARCH_PORT = 9200;

        public static GenericContainerResource<ElasticsearchContainer> createElasticsearchContainer() {
            final ElasticsearchContainer elasticsearchContainer =
                    new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.1").withPassword(PASSWORD);
            // Increase the timeout from 60 seconds to 90 seconds to ensure that it will be long enough
            // on the build pipeline
            elasticsearchContainer.setWaitStrategy(
                    new LogMessageWaitStrategy()
                            .withRegEx(".*(\"message\":\\s?\"started[\\s?|\"].*|] started\n$)")
                            .withStartupTimeout(Duration.ofSeconds(90)));
            return new GenericContainerResource<>(elasticsearchContainer,
                    resource -> {
                        resource.getContainer().caCertAsBytes().ifPresent(content -> {
                            try {
                                TemporaryFile tempFile = new TemporaryFile("elasticsearch.cafile", "http_ca", ".crt");
                                Files.write(tempFile.getPath(), content);
                                resource.addDependency(tempFile);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        resource.setProperty("elasticsearch.host", elasticsearchContainer.getHost());
                        resource.setProperty("elasticsearch.port", Integer.toString(elasticsearchContainer.getMappedPort(ELASTIC_SEARCH_PORT)));
                        resource.setProperty("elasticsearch.username", USER_NAME);
                        resource.setProperty("elasticsearch.password", PASSWORD);
                    }
            );
        }
    }
}