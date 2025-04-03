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
import org.apache.camel.spi.ManagementAgent;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.List;
import java.util.Set;

@Command(scope = "camel", name = "route-reset-stats", description = "Reset route performance stats from a Camel context")
@Service
public class RouteResetStats extends CamelCommandSupport implements Action {

    @Argument(index = 0, name = "context", description = "The name of the Camel context", required = true, multiValued = false)
    @Completion(CamelContextCompleter.class)
    String name;

    @Override
    public Object execute() throws Exception {

        List<CamelContext> camelContexts = getCamelContext(name);

        for (CamelContext camelContext : camelContexts) {
            ManagementAgent agent = camelContext.getManagementStrategy().getManagementAgent();
            if (agent != null) {
                MBeanServer mBeanServer = agent.getMBeanServer();

                // reset route mbeans
                ObjectName query = ObjectName.getInstance(agent.getMBeanObjectDomainName() + ":type=routes,*");
                Set<ObjectName> mBeans = mBeanServer.queryNames(query, null);
                for (ObjectName routeMBean : mBeans) {
                    String camelId = (String) mBeanServer.getAttribute(routeMBean, "CamelId");
                    if (camelId != null && camelId.equals(camelContext.getName())) {
                        mBeanServer.invoke(routeMBean, "reset", new Object[]{true}, new String[]{"boolean"});
                    }
                }
            }
        }

        return null;
    }

}
