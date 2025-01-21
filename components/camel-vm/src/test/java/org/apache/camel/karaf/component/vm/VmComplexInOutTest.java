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

package org.apache.camel.karaf.component.vm;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VmComplexInOutTest extends CamelTestSupport {

    @Test
    void testInOut() throws Exception {
        getMockEndpoint("mock:result-inner").expectedBodiesReceived("Bye World");
        getMockEndpoint("mock:result").expectedBodiesReceived("OK");

        String out = template.requestBody("direct:start", "Hello World", String.class);
        assertEquals("OK", out);

        MockEndpoint.assertIsSatisfied(context);
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                getContext().setTracing(true);

                from("direct:start").to("vm:a").setBody(constant("OK")).to("mock:result");

                from("vm:a").to("log:bar", "vm:b");
                from("vm:b").delay(10).to("direct:c");

                from("direct:c").transform(constant("Bye World")).to("mock:result-inner");
            }
        };
    }
}
