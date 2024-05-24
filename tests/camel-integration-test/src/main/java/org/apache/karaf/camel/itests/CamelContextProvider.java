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
     * Returns the {@link ProducerTemplate} associated with the given class according to the annotation
     * {@link CamelKarafTestHint}.
     */
    ProducerTemplate getTemplate(Class<?> clazz);
}
