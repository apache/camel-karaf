package org.apache.karaf.camel.itests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation uses to provide hints to the Camel Karaf test framework.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface CamelKarafTestHint {

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
}
