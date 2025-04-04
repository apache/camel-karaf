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
package org.apache.camel.karaf.shell;

import org.apache.camel.CamelContext;
import org.apache.camel.karaf.shell.completers.CamelContextCompleter;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.List;

@Command(scope = "camel", name = "rest-api-doc", description = "List the Camel REST services API documentation (requires camel-swagger-java on classpath)")
@Service
public class RestApiDoc extends CamelCommandSupport implements Action {

    @Argument(index = 0, name = "context", description = "The Camel context name where to look for the REST services", required = false, multiValued = false)
    @Completion(CamelContextCompleter.class)
    String name;

    @Override
    public Object execute() throws Exception {
        List<CamelContext> camelContexts = getCamelContext(name);

        if (camelContexts.size() != 1) {
            System.err.println("Camel context " + name + " not found");
            return null;
        }

        String json = camelContexts.get(0).getRestRegistry().apiDocAsJson();
        if (json != null) {
            System.out.println(json);
        } else {
            System.out.println("There is no REST service");
        }
        return null;
    }

}
