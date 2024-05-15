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
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.karaf.shell.api.action.*;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.List;

@Command(scope = "camel", name = "context-list", description = "List the Camel contexts")
@Service
public class ContextList extends CamelCommandSupport implements Action {

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("Context");
        table.column("Status");
        table.column("Total #");
        table.column("Failed #");
        table.column("Inflight #");
        table.column("Uptime");

        final List<CamelContext> camelContexts = getCamelContexts();

        for (CamelContext camelContext : camelContexts) {
            ManagedCamelContext mcc = camelContext.getCamelContextExtension().getContextPlugin(ManagedCamelContext.class);
            long exchangesTotal = 0;
            long exchangesInflight = 0;
            long exchangesFailed = 0;
            if (mcc != null && mcc.getManagedCamelContext() != null) {
                exchangesTotal = mcc.getManagedCamelContext().getExchangesTotal();
                exchangesInflight = mcc.getManagedCamelContext().getExchangesInflight();
                exchangesFailed = mcc.getManagedCamelContext().getExchangesFailed();
            }
            table.addRow().addContent(camelContext.getName(),
                    camelContext.getStatus().name(),
                    exchangesTotal,
                    exchangesFailed,
                    exchangesInflight,
                    camelContext.getUptime());
        }

        table.print(System.out);

        return null;
    }

}
