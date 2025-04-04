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
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.karaf.shell.completers.CamelContextCompleter;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.List;

@Command(scope = "camel", name = "route-list", description = "List Camel routes")
@Service
public class RouteList extends CamelCommandSupport implements Action {

    @Argument(index = 0, name = "context", description = "The Camel context name where to look for the route", required = false, multiValued = false)
    @Completion(CamelContextCompleter.class)
    String name;

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("Context");
        table.column("Route");
        table.column("Status");
        table.column("Total #");
        table.column("Failed #");
        table.column("Inflight #");
        table.column("Uptime");

        List<CamelContext> camelContexts = getCamelContext(name);

        for (CamelContext camelContext : camelContexts) {
            for (Route route : camelContext.getRoutes()) {
                ManagedCamelContext mcc = camelContext.getCamelContextExtension().getContextPlugin(ManagedCamelContext.class);
                long exchangesTotal = 0;
                long exchangesInflight = 0;
                long exchangesFailed = 0;
                if (mcc != null && mcc.getManagedCamelContext() != null) {
                    ManagedRouteMBean mr = mcc.getManagedRoute(route.getId());
                    exchangesFailed = mr.getExchangesFailed();
                    exchangesInflight = mr.getExchangesInflight();
                    exchangesTotal = mr.getExchangesTotal();
                }
                table.addRow().addContent(route.getCamelContext().getName(),
                        route.getId(),
                        getRouteState(route),
                        exchangesTotal,
                        exchangesFailed,
                        exchangesInflight,
                        route.getUptime());
            }
        }

        table.print(System.out);
        return null;
    }

    private String getRouteState(Route route) {
        // must use String type to be sure remote JMX can read the attribute without requiring Camel classes
        ServiceStatus status = route.getCamelContext().getRouteController().getRouteStatus(route.getId());
        if (status != null) {
            return status.name();
        }
        // assume started if not a ServiceSupport instance
        return ServiceStatus.Starting.name();
    }

}
