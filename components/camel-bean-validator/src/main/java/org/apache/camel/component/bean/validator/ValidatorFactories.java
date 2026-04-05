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
package org.apache.camel.component.bean.validator;

import jakarta.validation.Configuration;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ValidationProviderResolver;

import org.apache.camel.CamelContext;
import org.apache.camel.support.CamelContextHelper;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;

/**
 * OSGi-aware override of the upstream ValidatorFactories.
 *
 * In OSGi, Hibernate Validator 9.x's {@code ResourceBundleMessageInterpolator}
 * tries to discover the Jakarta EL {@code ExpressionFactory} via
 * {@code ServiceLoader}. This requires the TCCL to point to a classloader
 * that can find the {@code META-INF/services/jakarta.el.ExpressionFactory}
 * descriptor — which lives inside the Expressly bundle.
 *
 * This override resolves the Expressly bundle's classloader (via
 * HV's {@code DynamicImport-Package}) and sets it as the TCCL so that
 * the ServiceLoader-based discovery succeeds.
 */
public final class ValidatorFactories {

    private ValidatorFactories() {
    }

    public static ValidatorFactory buildValidatorFactory(
            CamelContext camelContext,
            boolean ignoreXmlConfiguration,
            ValidationProviderResolver validationProviderResolver,
            MessageInterpolator messageInterpolator,
            TraversableResolver traversableResolver,
            ConstraintValidatorFactory constraintValidatorFactory) {

        if (validationProviderResolver == null) {
            ValidationProviderResolverFactory factory
                    = CamelContextHelper.findSingleByType(camelContext, ValidationProviderResolverFactory.class);
            if (factory != null) {
                validationProviderResolver = factory.createValidationProviderResolver(camelContext);
            }
        }
        if (validationProviderResolver == null) {
            validationProviderResolver = new HibernateValidationProviderResolver();
        }

        // HV 9.x's ResourceBundleMessageInterpolator.buildExpressionFactory()
        // uses ServiceLoader via the TCCL to find the EL ExpressionFactory
        // implementation.  In OSGi the TCCL is typically null and no single
        // bundle classloader sees the Expressly META-INF/services descriptor.
        //
        // Resolve the Expressly bundle's classloader through HV's
        // DynamicImport-Package and set it as TCCL so ServiceLoader succeeds.
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        ClassLoader hvCl = HibernateValidator.class.getClassLoader();
        if (hvCl == null) {
            hvCl = ValidatorFactories.class.getClassLoader();
        }

        ClassLoader elCl = resolveExpresslyClassLoader(hvCl);
        Thread.currentThread().setContextClassLoader(elCl);
        try {
            HibernateValidatorConfiguration hvConfig = Validation.byProvider(HibernateValidator.class)
                    .providerResolver(validationProviderResolver)
                    .configure()
                    .externalClassLoader(elCl);

            if (messageInterpolator != null) {
                hvConfig.messageInterpolator(messageInterpolator);
            }
            if (traversableResolver != null) {
                hvConfig.traversableResolver(traversableResolver);
            }
            if (constraintValidatorFactory != null) {
                hvConfig.constraintValidatorFactory(constraintValidatorFactory);
            }
            if (ignoreXmlConfiguration) {
                hvConfig.ignoreXmlConfiguration();
            }

            return hvConfig.buildValidatorFactory();
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    /**
     * Resolve the Expressly bundle's classloader.  The hibernate-validator
     * wrapped bundle has {@code DynamicImport-Package} for
     * {@code org.glassfish.expressly}, so loading the implementation class
     * through HV's classloader triggers OSGi dynamic wiring and gives us
     * the bundle classloader that owns the SPI descriptor.
     */
    private static ClassLoader resolveExpresslyClassLoader(ClassLoader hvCl) {
        try {
            return hvCl.loadClass("org.glassfish.expressly.ExpressionFactoryImpl")
                    .getClassLoader();
        } catch (ClassNotFoundException e) {
            // Expressly not resolvable — fall back to HV's classloader
            return hvCl;
        }
    }
}
