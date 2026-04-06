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
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

/**
 * OSGi-aware override of the upstream ValidatorFactories.
 *
 * In OSGi, Hibernate Validator 9.x's {@code ResourceBundleMessageInterpolator}
 * tries to discover the Jakarta EL {@code ExpressionFactory} via
 * {@code ServiceLoader} / TCCL.  This fails in OSGi because no single
 * bundle classloader can resolve the Expressly SPI descriptor across
 * bundle boundaries.
 *
 * This override uses {@code ParameterMessageInterpolator} as the default
 * message interpolator when no custom one is provided, bypassing the EL
 * dependency entirely.  This is the approach recommended by Hibernate
 * Validator for environments where EL is not available.
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

        // In OSGi, EL-based message interpolation does not work because
        // ServiceLoader cannot discover ExpressionFactory across bundles.
        // Use ParameterMessageInterpolator as the default fallback.
        if (messageInterpolator == null) {
            messageInterpolator = new ParameterMessageInterpolator();
        }
        hvConfig.messageInterpolator(messageInterpolator);

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
}
