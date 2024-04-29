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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.junit.impl.ProbeRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A fork of {@link PaxExam} that supports external resources which can be created and destroyed outside Karaf.
 * <p>
 * This runner is intended to be used with {@link UseExternalResourceProvider} annotation in order to create the
 * external resources that are needed by the test. Please note that due to the way PaxExam works, the class cannot be
 * the same as the test class, but it can be a static inner class of it otherwise the class will need to be resolved
 * within Karaf which is what we want to avoid.
 *
 * @see UseExternalResourceProvider
 */
public class PaxExamWithExternalResource extends Runner implements Filterable, Sortable {
    private static final Logger LOG = LoggerFactory.getLogger(PaxExamWithExternalResource.class);
    private static final ThreadLocal<PaxExamWithExternalResource> current = new ThreadLocal<>();
    private final ParentRunner<?> delegate;
    private final List<ExternalResource> externalResources;

    public PaxExamWithExternalResource(Class<?> testClass) throws InitializationError, InvocationTargetException,
            IllegalAccessException {
        this.externalResources = beforeAll(testClass);
        try {
            current.set(this);
            this.delegate = new ProbeRunner(testClass);
        } finally {
            current.remove();
        }
    }

    private List<ExternalResource> beforeAll(Class<?> testClass) throws InvocationTargetException, IllegalAccessException {
        UseExternalResourceProvider annotation = testClass.getAnnotation(UseExternalResourceProvider.class);
        if (annotation != null) {
            List<ExternalResource> result = new ArrayList<>();
            for (Method m : annotation.value().getMethods()) {
                if (isExternalResourceSupplier(m)) {
                    ExternalResource externalResource = (ExternalResource) m.invoke(null);
                    externalResource.before();
                    result.add(externalResource);
                }
            }
            return result;
        }
        LOG.warn("Class {} is not annotated with @UseExternalResourceProvider", testClass.getName());
        return List.of();
    }

    private boolean isExternalResourceSupplier(Method m) {
        return ExternalResource.class.isAssignableFrom(m.getReturnType()) && m.getParameterTypes().length == 0
                && Modifier.isStatic(m.getModifiers());
    }

    @Override
    public Description getDescription() {
        return delegate.getDescription();
    }

    @Override
    public void run(RunNotifier notifier) {
        try {
            delegate.run(notifier);
        } finally {
            afterAll();
        }
    }

    private void afterAll() {
        for (int i = externalResources.size() - 1; i >= 0; i--) {
            try {
                externalResources.get(i).after();
            } catch (Exception e) {
                LOG.warn("Error while cleaning up external resource", e);
            }
        }
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        delegate.filter(filter);
    }

    @Override
    public void sort(Sorter sorter) {
        delegate.sort(sorter);
    }

    static Map<String, String> systemProperties() {
        PaxExamWithExternalResource value = current.get();
        if (value == null) {
            return Map.of();
        }
        return value.externalResources.stream()
                .map(ExternalResource::properties)
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
