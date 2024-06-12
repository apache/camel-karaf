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

import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.GenericContainerResource;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.apache.karaf.camel.itests.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

@CamelKarafTestHint(externalResourceProvider = CamelKafkaITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelKafkaITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    private static final Logger LOG = LoggerFactory.getLogger(CamelKafkaITest.class);

    private static final int KAFKA_PORT = 9092;

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {

        public static GenericContainerResource createKafkaContainer() {
            int exposedPort =  Utils.getNextAvailablePort();
            final GenericContainer<?> kafkaContainer =
                    new FixedHostPortGenericContainer<>("apache/kafka:3.7.0")
                        .withFixedExposedPort(exposedPort,exposedPort)
                        .withEnv("KAFKA_PROCESS_ROLES", "broker,controller")
                        .withEnv("KAFKA_NODE_ID", "1")
                        .withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@localhost:9093")
                        .withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER")
                        .withEnv("ALLOW_PLAINTEXT_LISTENER", "yes")
                        .withEnv("KAFKA_ADVERTISED_LISTENERS", "EXTERNAL_SAME_HOST://localhost:%s,INTERNAL://localhost:%s".formatted(exposedPort,KAFKA_PORT))
                        .withEnv("KAFKA_LISTENERS", "EXTERNAL_SAME_HOST://0.0.0.0:%s,INTERNAL://localhost:%s,CONTROLLER://:9093".formatted(exposedPort,KAFKA_PORT))
                        .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "CONTROLLER:PLAINTEXT,INTERNAL:PLAINTEXT,EXTERNAL_SAME_HOST:PLAINTEXT")
                        .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "INTERNAL")
                        .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                        // in case more logs needed
                        //.withEnv("KAFKA_LOG4J_LOGGERS","org.apache.kafka=DEBUG,kafka.request.logger=DEBUG,kafka.controller=DEBUG, kafka.coordinator=DEBUG,kafka.log=DEBUG,kafka.server=DEBUG, state.change.logger=DEBUG")
                        ;
            kafkaContainer.setWaitStrategy(
                    new LogMessageWaitStrategy()
                            .withRegEx(".*Kafka Server started.*")
                            .withStartupTimeout(Duration.ofSeconds(10)));

            return new GenericContainerResource(kafkaContainer, (Consumer<GenericContainerResource>) resource -> {
                try {
                   Container.ExecResult res = kafkaContainer.execInContainer("/opt/kafka/bin/kafka-topics.sh","--create",
                           "--topic", "testTopic", "--bootstrap-server" ,"localhost:%s".formatted(KAFKA_PORT));
                   LOG.info(res.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                resource.setProperty("kafka.port", Integer.toString(exposedPort));
            });
        }
    }
}