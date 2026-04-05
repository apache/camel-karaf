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

import java.util.Collections;
import java.util.Locale;

import jakarta.el.ExpressionFactory;
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
import org.hibernate.validator.internal.engine.messageinterpolation.DefaultLocaleResolver;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

/**
 * OSGi-aware override of the upstream ValidatorFactories.
 *
 * In OSGi, Hibernate Validator 9.x's {@code ResourceBundleMessageInterpolator}
 * tries to discover the Jakarta EL {@code ExpressionFactory} via
 * {@code ServiceLoader} / TCCL.  This fails in OSGi because no single
 * bundle classloader can find the Expressly SPI descriptor
 * ({@code META-INF/services/jakarta.el.ExpressionFactory}).
 *
 * This override directly instantiates the Expressly {@code ExpressionFactory}
 * (resolved via HV's {@code DynamicImport-Package}) and injects it into a
 * {@code ResourceBundleMessageInterpolator}, completely bypassing the
 * ServiceLoader discovery that does not work across OSGi bundles.
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

        ClassLoader hvCl = HibernateValidator.class.getClassLoader();
        if (hvCl == null) {
            hvCl = ValidatorFactories.class.getClassLoader();
        }

        HibernateValidatorConfiguration hvConfig = Validation.byProvider(HibernateValidator.class)
                .providerResolver(validationProviderResolver)
                .configure()
                .externalClassLoader(hvCl);

        // If no custom MessageInterpolator was provided, create one with
        // a directly-instantiated ExpressionFactory to bypass the broken
        // ServiceLoader discovery in OSGi.
        if (messageInterpolator == null) {
            ExpressionFactory ef = createExpressionFactory(hvCl);
            if (ef != null) {
                // Must use the 7-param constructor — the 3-param
                // (ResourceBundleLocator, boolean, ExpressionFactory)
                // constructor has a bug in HV 9.1.0 that ignores the
                // ExpressionFactory parameter and calls buildExpressionFactory().
                messageInterpolator = new ResourceBundleMessageInterpolator(
                        null, Collections.emptySet(), Locale.getDefault(),
                        new DefaultLocaleResolver(), true, false, ef);
            }
        }

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
    }

    /**
     * Directly instantiate the Expressly ExpressionFactory via HV's
     * DynamicImport-Package wiring, bypassing ServiceLoader entirely.
     */
    private static ExpressionFactory createExpressionFactory(ClassLoader hvCl) {
        try {
            Class<?> implClass = hvCl.loadClass("org.glassfish.expressly.ExpressionFactoryImpl");
            return (ExpressionFactory) implClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
