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
package org.apache.camel.karaf.shell.completers;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.karaf.shell.CamelCommandSupport;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

import java.util.List;

@Service
public class RouteCompleter extends CamelCommandSupport implements Completer {

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // grab selected camel context from the first argument
        String contextName = null;
        String[] args = commandLine.getArguments();
        if (args != null && args.length > 1) {
            // 0 is the command name itself
            // 1 is the first argument which is the camel context name
            contextName = args[1];
        }

        try {
            StringsCompleter delegate = new StringsCompleter();
            for (CamelContext camelContext : getCamelContext(contextName)) {
                for (Route route : camelContext.getRoutes()) {
                    delegate.getStrings().add(route.getRouteId());
                }
            }
            return delegate.complete(session, commandLine, candidates);
        } catch (Exception e) {
            // NA
        }
        return 0;
    }

}
