/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.camel.itests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the class that provides the methods to create all the external resources required by the test.
 * In the provider class, each public static method that returns an instance of a subtype of {@link ExternalResource}
 * with no parameters is considered as an {@link ExternalResource} supplier, so it will be invoked before executing
 * the test and {@code PaxExamWithExternalResource} will take care of its lifecycle making sure that it is created and
 * destroyed outside Karaf.
 *
 * @see PaxExamWithExternalResource
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface UseExternalResourceProvider {
    /**
     * The external resource provider class.
     */
    Class<?> value();
}
