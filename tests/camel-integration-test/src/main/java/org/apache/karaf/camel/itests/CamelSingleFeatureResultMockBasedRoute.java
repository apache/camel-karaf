package org.apache.karaf.camel.itests;

import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;

public interface CamelSingleFeatureResultMockBasedRoute extends CamelSingleFeatureRoute {

    default void setupMock() {
        MockEndpoint endpoint = getMockEndpoint();
        endpoint.setFailFast(false);
        configureMock(endpoint);
    }

    default void cleanMock() {
        CamelContext context = getContext();
        if (context != null) {
            MockEndpoint.resetMocks(context);
        }
    }

    default MockEndpoint getMockEndpoint() {
        return getContext().getEndpoint("mock:%s".formatted(getTestComponentName()), MockEndpoint.class);
    }

    default void configureMock(MockEndpoint mock) {
        mock.expectedMinimumMessageCount(1);
    }

    default void assertMockEndpointsSatisfied() throws InterruptedException {
        MockEndpoint.assertIsSatisfied(getContext());
    }
}