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

import java.util.Map;

/**
 * An interface representing an external resource to set up before a test and guarantee to tear it down afterward.
 * Compared to an {@link org.junit.rules.ExternalResource}, the methods {@link #before()} and {@link #after()} are
 * executed outside Karaf container, so it can be used to set up external resources like a database, a message broker,
 * etc.
 * @see TemporaryFile
 * @see GenericContainerResource
 */
public interface ExternalResource {

    /**
     * Sets up the external resource.
     */
    void before();

    /**
     * Tears down the external resource.
     */
    void after();

    /**
     * Gives access to the properties of the external resource like a username, a password or a path, that will be
     * provided to the Karaf instance as System properties.
     */
    Map<String, String> properties();
}
