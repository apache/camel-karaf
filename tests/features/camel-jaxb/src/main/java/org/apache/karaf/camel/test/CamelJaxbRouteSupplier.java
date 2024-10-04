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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.dataformat.JaxbDataFormat;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@Component(
        name = "karaf-camel-jaxb-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelJaxbRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    public static final String JAXB_BEAN = "jaxbBean";
    public static final String XML_SAMPLE_NAME = "Jax";
    public static final int XML_SAMPLE_AGE = 33;

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @Override
    public void configure(CamelContext context) {
        JaxbDataFormat jaxb = new JaxbDataFormat();
        jaxb.setContextPath(MyData.class.getName()); // org.apache.karaf.camel.test.CamelJaxbRouteSupplier$MyData
        jaxb.setContextPathIsClassName(Boolean.TRUE.toString());
        jaxb.setPrettyPrint(Boolean.FALSE.toString());
        jaxb.setFragment(Boolean.TRUE.toString()); // don't generate the XML declaration header

        context.getRegistry().bind(JAXB_BEAN, jaxb);
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        producerRoute.log("Will unmarshal: ${body}")
            .unmarshal(JAXB_BEAN)
            .log("Unmarshal: ${body}")
            .process(ex -> {
                MyData data = ex.getIn().getBody(MyData.class);
                assertEquals(XML_SAMPLE_NAME, data.getName());
                assertNull(data.getNickname());
                assertEquals(XML_SAMPLE_AGE, data.getAge());
            }).log("Will marshal: ${body}")
            .marshal(JAXB_BEAN)
            .log("Marshal: ${body}")
            .toF("mock:%s", getResultMockName());
    }

    @XmlRootElement(name = "MyData")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MyData {

        @XmlElement(required = true)
        private String name;

        @XmlElement(required = false)
        private String nickname;

        @XmlElement(required = true)
        private int age;

        public String getName() {
            return name;
        }

        public String getNickname() {
            return nickname;
        }

        public int getAge() {
            return age;
        }
    }
}