package org.apache.karaf.camel.itests;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.jetbrains.annotations.NotNull;

import static org.apache.karaf.camel.itests.Utils.toKebabCase;

public interface CamelSingleComponentRoute {

    default int getTimeoutInSeconds() {
        return 5;
    }

    default String getTestComponentName() {
        return getTestClassSimpleNameInKebabCase().replace("-itest", "-test");
    }

    default String getCamelFeatureName() {
        return getTestClassSimpleNameInKebabCase().replace("-itest", "");
    }

    @NotNull
    private String getTestClassSimpleNameInKebabCase() {
        String name = toKebabCase(this.getClass().getSimpleName());
        if (!name.endsWith("-itest")) {
            throw new IllegalArgumentException("The integration test class name doesn't match with the expected format: <tested-camel-component-name>ITest");
        }
        return name;
    }

    default String getTestBundleName() {
        return getTestComponentName();
    }

    default List<String> getRequiredFeatures() {
        return List.of(getCamelFeatureName());
    }

    default Processor getProcessorToCallOnSend() {
        return exchange -> exchange.getMessage().setBody(getBodyToSend());
    }

    default String getBodyToSend() {
        return getClass().getSimpleName();
    }

    CamelContext getContext();

    ProducerTemplate getTemplate();

    default void triggerProducerRoute() {
        Endpoint endpoint = getContext().hasEndpoint("direct:%s".formatted(getTestComponentName()));
        if (endpoint != null) {
            getTemplate().send(endpoint, getProcessorToCallOnSend());
        }
    }
}
