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
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

@CamelKarafTestHint(externalResourceProvider = CamelAws2KinesisITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelAws2KinesisITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("Hello Kinesis");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {

        private static final int LOCALSTACK_ORIGINAL_PORT = 4566;
        private static final String STREAM_NAME = "my-stream";
        private static final String ACCESS_KEY = "test";
        private static final String SECRET_KEY = "test";
        private static final String REGION = "us-east-1";
        public static GenericContainerResource<LocalStackContainer> createAws2KinesisContainer() {

            final LocalStackContainer localStackContainer =
                    new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.4.0"))
                            .withServices(LocalStackContainer.Service.KINESIS);

            return new GenericContainerResource<>(localStackContainer, resource -> {
                try {
                    localStackContainer.execInContainer("aws", "configure", "set", "aws_access_key_id", ACCESS_KEY, "--profile",
                            "localstack");
                    localStackContainer.execInContainer("aws", "configure", "set", "aws_secret_access_key", SECRET_KEY,
                            "--profile", "localstack");
                    localStackContainer.execInContainer("aws", "configure", "set", "region", REGION, "--profile", "localstack");
                    localStackContainer.execInContainer("aws",
                            "--endpoint-url=http://" + localStackContainer.getHost() + ":" + LOCALSTACK_ORIGINAL_PORT, "kinesis",
                            "create-stream", "--stream-name", STREAM_NAME, "--shard-count", "1", "--region", REGION,
                            "--profile", "localstack");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                resource.setProperty("localstack.kinesis.host", localStackContainer.getHost());
                resource.setProperty("localstack.kinesis.port",
                        Integer.toString(localStackContainer.getMappedPort(LOCALSTACK_ORIGINAL_PORT)));
                resource.setProperty("localstack.kinesis.accessKey", ACCESS_KEY);
                resource.setProperty("localstack.kinesis.secretKey", SECRET_KEY);
                resource.setProperty("localstack.kinesis.region", REGION);
                resource.setProperty("localstack.kinesis.streamName", STREAM_NAME);
            });
        }
    }
}