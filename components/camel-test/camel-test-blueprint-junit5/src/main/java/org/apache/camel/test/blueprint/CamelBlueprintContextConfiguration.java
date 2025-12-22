package org.apache.camel.test.blueprint;

import org.apache.camel.test.junit5.CamelContextConfiguration;

public class CamelBlueprintContextConfiguration extends CamelContextConfiguration {

    public CamelBlueprintContextConfiguration withBlueprintCamelContextSupplier(
            CamelContextSupplier camelContextSupplier) {
        withCamelContextSupplier(camelContextSupplier);
        return this;
    }

    public CamelBlueprintContextConfiguration withBlueprintPostProcessor(
            PostProcessor postProcessor) {
        withPostProcessor(postProcessor);
        return this;
    }

    protected CamelBlueprintContextConfiguration withBlueprintRoutesSupplier(
            RoutesSupplier routesSupplier) {
        withRoutesSupplier(routesSupplier);
        return this;
    }
}
