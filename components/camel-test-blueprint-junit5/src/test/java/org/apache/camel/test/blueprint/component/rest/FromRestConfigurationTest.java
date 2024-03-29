/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.test.blueprint.component.rest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FromRestConfigurationTest extends FromRestGetTest {

    @Override
    protected String getBlueprintDescriptor() {
        return "org/apache/camel/test/blueprint/component/rest/FromRestConfigurationTest.xml";
    }

    @Override
    @Test
    public void testFromRestModel() throws Exception {
        super.testFromRestModel();

        assertEquals("dummy-rest", context.getRestConfiguration().getComponent());
        assertEquals("localhost", context.getRestConfiguration().getHost());
        assertEquals(9090, context.getRestConfiguration().getPort());
        assertEquals("bar", context.getRestConfiguration().getComponentProperties().get("foo"));
        assertEquals("stuff", context.getRestConfiguration().getComponentProperties().get("other"));
        assertEquals("200", context.getRestConfiguration().getEndpointProperties().get("size"));
        assertEquals("1000", context.getRestConfiguration().getConsumerProperties().get("pollTimeout"));
    }

}
