/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.karaf.camel.itests;

import java.util.function.Function;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;

import static org.apache.karaf.camel.itests.Utils.toKebabCase;

/**
 * The parent class of the camel route suppliers that test a specific Camel component/wrapper/feature.
 * <p>
 * It creates and starts two simple Camel routes, the first one if {@link #producerEnabled()} returns {@code true}
 * calls the producer of the tested Camel component and the second one if {@link #consumerEnabled()} returns {@code true}
 * consumes what has been produced by the first route thanks to the consumer corresponding to the tested Camel component.
 */
public abstract class AbstractCamelSingleComponentRouteSupplier implements CamelRouteSupplier {

    public String getTestComponentName() {
        String name = toKebabCase(this.getClass().getSimpleName());
        if (!name.endsWith("-route-supplier")) {
            throw new IllegalArgumentException("The route supplier class name doesn't match with the expected format: <tested-camel-component-name>RouteSupplier");
        }
        return name.replace("-route-supplier", "-test");
    }

    @Override
    public void createRoutes(RouteBuilder builder) {
        if (producerEnabled()) {
            configureProducer(
                builder, builder.fromF("direct:%s", getTestComponentName()).routeId("producer-%s".formatted(getTestComponentName()))
            );
        }
        if (consumerEnabled()) {
            configureConsumer(consumerRoute().apply(builder));
        }
    }

    /**
     * Indicates whether the Camel component that is tested has a producer. Default is {@code true}.
     */
    protected boolean producerEnabled() {
        return true;
    }

    /**
     * Indicates whether the Camel component that is tested has a consumer. Default is {@code true}.
     */
    protected boolean consumerEnabled() {
        return true;
    }

    /**
     * Returns the function used to build the Camel route to test the consumer. Default is {@code null}.
     */
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {
        return null;
    }

    /**
     * Executes the code needed to configure the Camel route to test the producer.
     */
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        // Do nothing by default
    }

    /**
     * Executes the code needed to configure the Camel route to test the consumer.
     */
    protected void configureConsumer(RouteDefinition consumerRoute) {
        if (consumerRoute.hasCustomIdAssigned()) {
            return;
        }
        consumerRoute.routeId("consumer-%s".formatted(getTestComponentName()));
    }
}
