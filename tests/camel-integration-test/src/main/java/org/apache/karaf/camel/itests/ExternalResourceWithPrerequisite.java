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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ExternalResourceWithPrerequisite implements ExternalResource {

    private final Map<String, String> properties = new HashMap<>();

    @Override
    public void before() {
        for (ExternalResource prerequisite : getPrerequisites()) {
            prerequisite.before();
            prerequisite.properties().forEach(this::setProperty);
        }
        doStart();
    }

    @Override
    public void after() {
        doStop();
        for (ExternalResource prerequisite : getPrerequisites()) {
            prerequisite.after();
        }
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    @Override
    public Map<String, String> properties() {
        return properties;
    }

    protected abstract List<ExternalResource> getPrerequisites();

    protected abstract void doStart();

    protected abstract void doStop();

}
