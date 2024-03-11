/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.camel.test;

import org.apache.camel.builder.RouteBuilder;
import org.apache.karaf.camel.itests.AbstractCamelComponent;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "karaf-camel-seda-test",
        immediate = true
)
public class CamelSedaComponent extends AbstractCamelComponent {
    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
                @Override
                public void configure() throws Exception {

                    from("timer://starter?repeatCount=1")
                            .setBody(constant("OK")).to("direct:start");
                    from("direct:start").autoStartup(true)
                            .log("calling seda next")
                            // send it to the seda queue that is async
                            .to("seda:next")
                            // return a constant response
                            .transform(constant("OK"));
                    from("seda:next").autoStartup(true)
                            .log("seda next called")
                            .to("file:"+System.getProperty("project.target")+"?fileName=testResult.txt"); // Write the message to a file

                }
        };
    }
}