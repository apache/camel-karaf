package org.apache.karaf.camel.itests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * The annotation uses to provide hints to the Camel Karaf test framework.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface CamelKarafTestHint {

    /**
     * The default timeout in seconds for the test.
     */
    int DEFAULT_TIMEOUT = 300;

    /**
     * Specify the class that provides the methods to create all the external resources required by the test.
     * In the provider class, each public static method that returns an instance of a subtype of {@link ExternalResource}
     * with no parameters is considered as an {@link ExternalResource} supplier, so it will be invoked before executing
     * the test and {@code PaxExamWithExternalResource} will take care of its lifecycle making sure that it is created and
     * destroyed outside Karaf.
     *
     * @see PaxExamWithExternalResource
     */
    Class<?> externalResourceProvider() default Object.class;

    /**
     * Indicates whether the test is a blueprint test or not. By default, it's not a blueprint test.
     * @return {@code true} if the test is a blueprint test, {@code false} otherwise
     */
    boolean isBlueprintTest() default false;

    /**
     * Specify the list of additional features required by the test.
     */
    String[] additionalRequiredFeatures() default {};

    /**
     * Specify the name of the Camel context to use in the test.
     */
    String camelContextName() default "";

    /**
     * Specify the list of Camel route suppliers to use within the context of the test. By default, all detected
     * Camel route suppliers are used.
     */
    String[] camelRouteSuppliers() default {};

    /**
     * Forces to ignore all Camel route suppliers within the context of the tests. False by default.
     */
    boolean ignoreRouteSuppliers() default false;

    /**
     * Specify whether the test should be retried on failure. By default, no retry is performed.
     */
    boolean retryOnFailure() default false;

    /**
     * Specify the timeout in seconds for the test. By default, the timeout is 300 seconds.
     */
    int timeout() default DEFAULT_TIMEOUT;
}
