package org.apache.karaf.camel.test;

import java.util.function.Function;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.RestConfiguration;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "karaf-camel-rest-netty-http-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelRestNettyHttpRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    private static final String REST_PORT = "rest.port";

    @Override
    protected String getResultMockName() {
        return "camel-netty-http-test";
    }

    @Override
    public String getTestComponentName() {
        return "camel-netty-http-test";
    }

    @Override
    public void configure(CamelContext camelContext) {
        RestConfiguration config = new RestConfiguration();
        config.setComponent("netty-http");
        config.setHost("127.0.0.1");
        config.setPort(Integer.parseInt(System.getProperty(REST_PORT)));
        camelContext.setRestConfiguration(config);
    }

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return builder ->
                builder.fromF("rest:get:testRestNetty")
                        .log("receiving rest http request")
                        .setBody(builder.constant("OK"));
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.log("calling http endpoint")
                .setBody(builder.constant("OK"))
                .log("sending rest http request")
                .toF("rest:get:testRestNetty?host=127.0.0.1:%s", System.getProperty(REST_PORT));
    }
}
