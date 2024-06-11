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

import static org.apache.camel.builder.Builder.constant;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.HazelcastOperation;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@Component(name = "karaf-camel-hazelcast-test", immediate = true, service = CamelRouteSupplier.class)
public class CamelHazelcastRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {


    @Override
    public void configure(CamelContext camelContext) {
        System.setProperty("hazelcast.shutdownhook.enabled","false");
        Config hazelcastConfig = new Config();
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        camelContext.getRegistry().bind("hzInstance", hazelcastInstance);
    }

    @Override
    protected boolean consumerEnabled() {
        //consumers are implemented directly in the configureProducer method here
        return false;
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {

        configureConsumer(builder.from("hazelcast-list:myList?hazelcastInstance=#hzInstance").log("list received ${body}").setBody(constant("OK_List")));
        configureConsumer(builder.from("hazelcast-map:myMap?hazelcastInstance=#hzInstance").log("map received ${body}").setBody(constant("OK_Map")));
        configureConsumer(builder.from("hazelcast-replicatedmap:myRMap?hazelcastInstance=#hzInstance").log("rmap received ${body}").setBody(constant("OK_RMap")));
        configureConsumer(builder.from("hazelcast-queue:myQueue?hazelcastInstance=#hzInstance").log("queue received ${body}").setBody(constant("OK_Queue")));
        configureConsumer(builder.from("hazelcast-set:mySet?hazelcastInstance=#hzInstance").log("set received ${body}").setBody(constant("OK_Set")));
        configureConsumer(builder.from("hazelcast-topic:myTopic?hazelcastInstance=#hzInstance").log("topic received ${body}").setBody(constant("OK_Topic")));

        producerRoute
                .log("insert in Hz MAP")
                //Map
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.PUT))
                .setHeader(HazelcastConstants.OBJECT_ID, constant("key"))
                .setBody(builder.constant("OK"))
                .to("hazelcast-map:myMap?hazelcastInstance=hzInstance")

                //Replicated Map
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.PUT))
                .setHeader(HazelcastConstants.OBJECT_ID, constant("key"))
                .setBody(builder.constant("OK"))
                .to("hazelcast-replicatedmap:myRMap?hazelcastInstance=hzInstance")

                //List
                .log("insert in Hz List")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.ADD))
                .to("hazelcast-list:myList?hazelcastInstance=hzInstance")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.GET))
                .setHeader(HazelcastConstants.OBJECT_POS, constant(0))
                .to("hazelcast-list:myList?hazelcastInstance=hzInstance")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.CLEAR))
                .to("hazelcast-list:myList?hazelcastInstance=hzInstance")

                //Queue
                .log("insert in Hz Queue")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.PUT))
                .to("hazelcast-queue:myQueue?hazelcastInstance=hzInstance")

                //Ring
                .log("insert in Hz Ring")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.ADD))
                .to("hazelcast-ringbuffer:myRing?hazelcastInstance=hzInstance")

                //Set
                .log("insert in Hz Set")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.ADD))
                .to("hazelcast-set:mySet?hazelcastInstance=hzInstance")

                //Topic
                .log("insert in Hz Topic")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.PUBLISH))
                .to("hazelcast-topic:myTopic?hazelcastInstance=hzInstance")

                //Atomic Num
                .log("Create Atomic Map")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.INCREMENT))
                .to("hazelcast-atomicvalue:myNum?hazelcastInstance=hzInstance")

                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.GET))
                .toF("hazelcast-atomicvalue:myNum?hazelcastInstance=hzInstance", HazelcastConstants.ATOMICNUMBER_PREFIX)

                .log("End producer route");
    }
}