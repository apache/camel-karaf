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
import org.apache.camel.karaf.shell.completers.RouteCompleter;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "camel", name = "route-stop", description = "Stop a Camel route")
@Service
public class RouteStop extends CamelCommandSupport implements Action {

    @Argument(index = 0, name = "context", description = "The Camel context name", required = true, multiValued = false)
    @Completion(CamelContextCompleter.class)
    String context;

    @Argument(index = 1, name = "route", description = "The Camel route ID or a wildcard expression", required = true, multiValued = false)
    @Completion(RouteCompleter.class)
    String route;

    @Override
    public Object execute() throws Exception {
        CamelContext camelContext = getCamelContext(context);
        if (camelContext == null) {
            System.err.println("Camel context " + context + " not found");
            return null;
        }

        camelContext.getRouteController().stopRoute(route);

        return null;
    }

}
