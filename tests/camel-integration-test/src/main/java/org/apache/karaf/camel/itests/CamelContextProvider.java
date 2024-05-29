package org.apache.karaf.camel.itests;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

public interface CamelContextProvider {

    /**
     * Returns the {@link CamelContext} associated with the given class according to the annotation
     * {@link CamelKarafTestHint}.
     */
    CamelContext getContext(Class<?> clazz);

    /**
     * Returns the {@link CamelContext} associated with the current class according to the annotation
     * {@link CamelKarafTestHint}.
     */
    default CamelContext getContext() {
        return getContext(getClass());
    }

    /**
     * Returns the {@link CamelContext} associated with the given name and type of test
     */
    CamelContext getContext(String name, boolean isBlueprintTest);

    /**
     * Returns the {@link CamelContext} associated with the given name for a blueprint test
     */
    default CamelContext getContext(String name) {
        return getContext(name, true);
    }

    /**
     * Returns the {@link ProducerTemplate} associated with the given class according to the annotation
     * {@link CamelKarafTestHint}.
     */
    ProducerTemplate getTemplate(Class<?> clazz);

    /**
     * Returns the {@link ProducerTemplate} associated with the current class according to the annotation
     * {@link CamelKarafTestHint}.
     */
    default ProducerTemplate getTemplate() {
        return getTemplate(getClass());
    }

    /**
     * Returns the {@link ProducerTemplate} associated with the given name and type of test
     */
    ProducerTemplate getTemplate(String name, boolean isBlueprintTest);

    /**
     * Returns the {@link ProducerTemplate} associated with the given name for a blueprint test
     */
    default ProducerTemplate getTemplate(String name) {
        return getTemplate(name, true);
    }
}
