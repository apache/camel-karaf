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
import org.apache.camel.spi.ManagementAgent;
import org.apache.karaf.shell.api.action.*;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Command(scope = "camel", name = "context-inflight", description = "List inflight exchanges")
@Service
public class ContextInflight extends CamelCommandSupport implements Action {

    @Argument(index = 0, name = "context", description = "The Camel context name", required = false, multiValued = false)
    @Completion(CamelContextCompleter.class)
    String name;

    @Option(name = "--limit", aliases = "-l", description = "To limit the number of exchanges shown", required = false, multiValued = false)
    int limit = -1;

    @Argument(index = 1, name = "route", description = "The Camel route ID", required = false, multiValued = false)
    @Completion(RouteCompleter.class)
    String route;

    @Option(name = "--sort", aliases = "-s", description = "Sort by longest duration (true) or by exchange id (false)", required = false, multiValued = false, valueToShowInHelp = "false")
    boolean sortByLongestDuration;

    @Override
    public Object execute() throws Exception {
        List<CamelContext> camelContexts = getCamelContext(name);

        ShellTable table = new ShellTable();
        table.column("ExchangeId");
        table.column("From Route");
        table.column("Context");
        table.column("Route");
        table.column("Node");
        table.column("Elapsed (ms)");
        table.column("Duration (ms)");

        for (CamelContext camelContext : camelContexts) {
            ManagementAgent agent = camelContext.getManagementStrategy().getManagementAgent();
            if (agent != null) {
                MBeanServer mBeanServer = agent.getMBeanServer();
                ObjectName on = new ObjectName(agent.getMBeanObjectDomainName() + ":type=services,name=DefaultInflightRepository,context=" + camelContext.getManagementName());
                if (mBeanServer.isRegistered(on)) {
                    TabularData list = (TabularData) mBeanServer.invoke(on, "browse", new Object[]{route, limit, sortByLongestDuration}, new String[]{"java.lang.String", "int", "boolean"});
                    Collection<CompositeData> values = (Collection<CompositeData>) list.values();
                    for (CompositeData data : values) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        Object exchangeId = data.get("exchangeId");
                        Object fromRouteId = data.get("fromRouteId");
                        Object routeId = data.get("routeId");
                        Object nodeId = data.get("nodeId");
                        Object elapsed = data.get("elapsed");
                        Object duration = data.get("duration");
                        table.addRow().addContent(exchangeId, fromRouteId, camelContext.getName(), routeId, nodeId, elapsed, duration);
                    }
                }
            }
        }

        table.print(System.out);
        return null;
    }

}
