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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A class that provides available ports for the test.
 */
public class AvailablePortProvider implements ExternalResource {

    private static final Set<Integer> USED_PORTS = ConcurrentHashMap.newKeySet();

    private final Map<String, Integer> ports = new ConcurrentHashMap<>();

    private final List<String> neededPortPropertyNames;

    /**
     * Create an AvailablePortProvider with the given name of property that will have the needed ports for the test as
     * value.
     * @param neededPortPropertyNames the name of the property that will have the needed ports as value
     */
    public AvailablePortProvider(List<String> neededPortPropertyNames) {
        this.neededPortPropertyNames = neededPortPropertyNames;
    }

    @Override
    public void before() {
        for (String name : neededPortPropertyNames) {
            Utils.getNextAvailablePort(port -> {
                if (USED_PORTS.add(port)) {
                    ports.put(name, port);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void after() {
        ports.values().forEach(USED_PORTS::remove);
    }

    @Override
    public Map<String, String> properties() {
        return ports.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    }
}
