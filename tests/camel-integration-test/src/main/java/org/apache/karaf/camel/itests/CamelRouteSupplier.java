package org.apache.karaf.camel.itests;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;

/**
 * The interface that provides the ability to supply Camel routes to create and start in the Camel context.
 */
public interface CamelRouteSupplier {

    /**
     * Configures the Camel context before creating the routes.
     *
     * @param camelContext the Camel context
     */
    default void configure(CamelContext camelContext) {
        // Do nothing by default
    }

    /**
     * Creates the Camel routes in the Camel context.
     *
     * @param builder the Camel route builder
     */
    void createRoutes(RouteBuilder builder);

    /**
     * Cleans up the Camel context before removing the routes.
     */
    default void cleanUp(CamelContext camelContext) {
        // Do nothing by default
    }
}
