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
import org.apache.camel.Endpoint;
import org.apache.camel.ServiceStatus;
import org.apache.camel.StatefulService;
import org.apache.camel.karaf.shell.completers.CamelContextCompleter;
import org.apache.camel.util.URISupport;
import org.apache.karaf.shell.api.action.*;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Command(scope = "camel", name = "endpoint-list", description = "Lists the Camel endpoints")
@Service
public class EndpointList extends CamelCommandSupport implements Action {

    @Argument(index = 0, name = "context", description = "The name of the Camel context (support wildcard)", required = true, multiValued = false)
    @Completion(CamelContextCompleter.class)
    String name;

    @Option(name = "--decode", aliases = "-d", description = "Whether to decode the endpoint uri so its human readable", required = false, multiValued = false, valueToShowInHelp = "true")
    boolean decode = true;

    @Override
    public Object execute() throws Exception {

        ShellTable table = new ShellTable();
        table.column("Context");
        table.column("Uri");
        table.column("Status");

        CamelContext camelContext = getCamelContext(name);

        if (camelContext == null) {
            System.err.println("Camel context " + name + " not found");
            return null;
        }

        List<Endpoint> endpoints = new ArrayList<>(camelContext.getEndpoints());
        // sort routes
        Collections.sort(endpoints, new Comparator<Endpoint>() {
            @Override
            public int compare(Endpoint e1, Endpoint e2) {
                return e1.getEndpointKey().compareTo(e2.getEndpointKey());
            }
        });
        for (Endpoint endpoint : endpoints) {
            String uri = endpoint.getEndpointUri();
            if (decode) {
                // decode uri so its more human readable
                uri = URLDecoder.decode(uri, "UTF-8");
            }
            // sanitize and mask uri so we don't see passwords
            uri = URISupport.sanitizeUri(uri);
            table.addRow().addContent(camelContext.getName(), uri, getEndpointState(endpoint));
        }

        table.print(System.out);
        return null;
    }

    private static String getEndpointState(Endpoint endpoint) {
        // must use String type to be sure remote JMX can read the attribute without requiring Camel classes
        if (endpoint instanceof StatefulService) {
            ServiceStatus status = ((StatefulService) endpoint).getStatus();
            return status.name();
        }

        // assume started if not a ServiceSupport instance
        return ServiceStatus.Started.name();
    }

}
