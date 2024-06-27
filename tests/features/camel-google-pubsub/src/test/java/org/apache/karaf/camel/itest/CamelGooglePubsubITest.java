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

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.GenericContainerResource;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.TopicName;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@CamelKarafTestHint(externalResourceProvider = CamelGooglePubsubITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelGooglePubsubITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {

    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("Hello, Google Pub/Sub!");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {

        private static final int PUB_SUB_PORT = 8085;
        static final String TEST_PROJECT_ID = "test-project";
        static final String TEST_TOPIC = "test-topic";
        static final String TEST_SUBSCRIPTION = "test-subscription";

        public static GenericContainerResource<PubSubEmulatorContainer> createPubsubContainer() {
            final PubSubEmulatorContainer pubSubEmulatorContainer = new PubSubEmulatorContainer(
                    DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:441.0.0-emulators"));

            return new GenericContainerResource<>(pubSubEmulatorContainer, resource -> {
                setUpEmulator(pubSubEmulatorContainer);

                resource.setProperty("pubsub.host", pubSubEmulatorContainer.getHost());
                resource.setProperty("pubsub.port", Integer.toString(pubSubEmulatorContainer.getMappedPort(PUB_SUB_PORT)));
                resource.setProperty("pubsub.project", TEST_PROJECT_ID);
                resource.setProperty("pubsub.topic", TEST_TOPIC);
                resource.setProperty("pubsub.subscription", TEST_SUBSCRIPTION);
            });
        }

        private static void createTopic(final TransportChannelProvider channelProvider,
                final NoCredentialsProvider credentialsProvider) throws IOException {
            final TopicAdminSettings topicAdminSettings = TopicAdminSettings.newBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build();
            try (final TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
                final TopicName topicName = TopicName.of(TEST_PROJECT_ID, TEST_TOPIC);
                topicAdminClient.createTopic(topicName);
            }
        }

        private static void createSubscription(final TransportChannelProvider channelProvider,
                final NoCredentialsProvider credentialsProvider) throws IOException {
            final SubscriptionAdminSettings subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build();

            try (final SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(
                    subscriptionAdminSettings)) {
                final SubscriptionName subscriptionName = SubscriptionName.of(TEST_PROJECT_ID, TEST_SUBSCRIPTION);
                subscriptionAdminClient.createSubscription(subscriptionName, TopicName.of(TEST_PROJECT_ID, TEST_TOPIC),
                        PushConfig.getDefaultInstance(), 10);
            }
        }

        static void setUpEmulator(final PubSubEmulatorContainer emulator) {
            final String endpoint = emulator.getEmulatorEndpoint();
            final ManagedChannel channel = ManagedChannelBuilder.forTarget(endpoint).usePlaintext().build();
            try {
                final TransportChannelProvider channelProvider =
                        FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
                final NoCredentialsProvider credentialsProvider = NoCredentialsProvider.create();

                createTopic(channelProvider, credentialsProvider);
                createSubscription(channelProvider, credentialsProvider);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            } finally {
                channel.shutdown();
            }
        }
    }
}