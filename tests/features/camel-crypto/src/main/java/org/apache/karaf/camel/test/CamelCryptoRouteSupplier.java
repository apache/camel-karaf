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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.crypto.DigitalSignatureConstants;
import org.apache.camel.model.RouteDefinition;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.apache.karaf.camel.itests.CamelRouteSupplier;
import org.osgi.service.component.annotations.Component;

@Component(
        name = "karaf-camel-crypto-test",
        immediate = true,
        service = CamelRouteSupplier.class
)
public class CamelCryptoRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    protected boolean consumerEnabled() {
        return false;
    }

    @Override
    protected void configureProducer(RouteBuilder builder, RouteDefinition producerRoute) {
        KeyPair keys = getKeyPair();

        producerRoute.log("Will sign: ${body}")
            .setHeader(DigitalSignatureConstants.SIGNATURE_PRIVATE_KEY, constant(keys.getPrivate()))
            .to("crypto:sign:bobsPizzaSignature")
            .log("Signature: ${header.%s}".formatted(DigitalSignatureConstants.SIGNATURE))
            .log("Will verify: ${header.%s}".formatted(DigitalSignatureConstants.SIGNATURE))
            .setHeader(DigitalSignatureConstants.SIGNATURE_PUBLIC_KEY_OR_CERT, constant(keys.getPublic()))
            .to("crypto:verify:bobsPizzaSignature?clearHeaders=false")
            .log("Verified: ${body}")
            .toF("mock:%s", getResultMockName());
    }

    private KeyPair getKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(512, new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}