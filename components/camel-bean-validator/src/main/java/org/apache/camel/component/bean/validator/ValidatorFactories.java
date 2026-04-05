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
 * In OSGi, Hibernate Validator's {@code externalClassLoader} defaults to
 * {@code null} and the TCCL is typically null.  This causes HV000116 /
 * HV000183 when Hibernate Validator tries to initialise the Jakarta EL
 * {@code ExpressionFactory}.
 *
 * This override sets the TCCL to the Hibernate Validator bundle's class
 * loader before any validation calls.  That bundle carries a
 * {@code DynamicImport-Package} for the Expressly EL implementation,
 * allowing {@code ExpressionFactory.newInstance()} to discover it.
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

        // In OSGi the TCCL is typically null.  Hibernate Validator uses it
        // both as a fallback for externalClassLoader and to initialise the
        // EL ExpressionFactory.  Set it to the HV bundle's class loader
        // which has DynamicImport-Package for the Expressly EL impl.
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        ClassLoader hvCl = HibernateValidator.class.getClassLoader();
        if (hvCl == null) {
            // Fallback: use our own bundle classloader
            hvCl = ValidatorFactories.class.getClassLoader();
        }
        Thread.currentThread().setContextClassLoader(hvCl);
        try {
            HibernateValidatorConfiguration hvConfig = Validation.byProvider(HibernateValidator.class)
                    .providerResolver(validationProviderResolver)
                    .configure()
                    .externalClassLoader(hvCl);

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
}
