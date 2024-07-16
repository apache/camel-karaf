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
import org.testcontainers.containers.wait.strategy.Wait;

@CamelKarafTestHint(externalResourceProvider = CamelDockerITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelDockerITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    private static final int ORIGINAL_DOCKER_NO_TLS_PORT = 2375;

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK_PING", "OK_PULL_IMAGE", "OK_REMOVE_IMAGE");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {
        public static GenericContainerResource<DockerContainer> createDockerContainer() {

            DockerContainer dockerContainer = new DockerContainer("docker:27.0");
            return new GenericContainerResource<>(dockerContainer, resource -> {
                resource.setProperty("docker.host", dockerContainer.getHost());
                resource.setProperty("docker.port", Integer.toString(dockerContainer.getMappedPort(ORIGINAL_DOCKER_NO_TLS_PORT)));
            });
        }
    }

    public static class DockerContainer extends GenericContainer<DockerContainer> {

        public DockerContainer(final String containerName) {
            super(containerName);

            // is mandatory to run this image with --privileged    
            withPrivilegedMode(true)
                // expose docker in docker container daemon on TCP without SSL
                .withCommand("dockerd", "--host", "tcp://0.0.0.0:%s".formatted(ORIGINAL_DOCKER_NO_TLS_PORT), "--tls=false")
                .withExposedPorts(ORIGINAL_DOCKER_NO_TLS_PORT)
                .waitingFor(Wait.forListeningPort());
        }
    }
}