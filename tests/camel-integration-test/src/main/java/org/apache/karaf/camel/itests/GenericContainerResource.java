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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

/**
 * A JUnit ExternalResource that starts and stops a TestContainer.
 *
 * @param <T> the type of the TestContainer
 */
public class GenericContainerResource<T extends GenericContainer<T>> implements ExternalResource {

    private static final Logger LOG = LoggerFactory.getLogger(GenericContainerResource.class);
    private final T container;
    private final Map<String, String> properties = new HashMap<>();
    private final List<ExternalResource> dependencies = new ArrayList<>();
    private final Consumer<GenericContainerResource<T>> onStarted;

    public GenericContainerResource(T container) {
        this(container, t -> {});
    }

    /**
     * Create a GenericContainerResource with the given TestContainer and a callback to be called when the container is
     * started.
     * @param container the TestContainer
     * @param onStarted the callback to be called when the container is started
     */
    public GenericContainerResource(T container, Consumer<GenericContainerResource<T>> onStarted) {
        this.container = container;
        this.onStarted = onStarted;
    }

    @Override
    public void before() {
        container.start();
        onStarted.accept(this);
        for (ExternalResource dependency : dependencies) {
            dependency.properties().forEach(this::setProperty);
        }
        LOG.info("Container {} started", container.getDockerImageName());
    }

    @Override
    public void after() {
        container.stop();
        for (ExternalResource dependency : dependencies) {
            try {
                dependency.after();
            } catch (Exception e) {
                LOG.warn("Error cleaning dependency: {}", dependency.getClass().getName(), e);
            }
        }
        LOG.info("Container {} stopped", container.getDockerImageName());
    }

    @Override
    public Map<String, String> properties() {
        return properties;
    }

    public T getContainer() {
        return container;
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public void addDependency(ExternalResource dependency) {
        dependencies.add(dependency);
    }
}
