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
import org.apache.camel.spi.RestRegistry;
import org.apache.camel.util.URISupport;
import org.apache.karaf.shell.api.action.*;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Command(scope = "camel", name = "rest-registry-list", description = "Lists all Camel REST services enlisted in the Rest Registry from a Camel context")
@Service
public class RestRegistryList extends CamelCommandSupport implements Action {

    @Argument(index = 0, name = "context", description = "The Camel context name where to look for the REST services", required = true, multiValued = false)
    @Completion(CamelContextCompleter.class)
    String name;

    @Option(name = "--decode", aliases = "-d", description = "Whether to decode the endpoint uri so it's human readable", required = false, multiValued = false, valueToShowInHelp = "true")
    boolean decode = true;

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("Url");
        table.column("Base Path");
        table.column("Uri Template");
        table.column("Method");
        table.column("State");

        List<CamelContext> camelContexts = getCamelContext(name);
        if (camelContexts.size() != 1) {
            System.err.println("Camel context " + name + " not found");
            return null;
        }

        List<RestRegistry.RestService> services = new ArrayList<>(camelContexts.get(0).getRestRegistry().listAllRestServices());
        Collections.sort(services, new Comparator<RestRegistry.RestService>() {
            @Override
            public int compare(RestRegistry.RestService s1, RestRegistry.RestService s2) {
                return s1.getUrl().compareTo(s2.getUrl());
            }
        });
        for (RestRegistry.RestService service : services) {
            String uri = service.getUrl();
            if (decode) {
                // decode uri so it's more human readable
                uri = URLDecoder.decode(uri, "UTF-8");
            }
            // sanitize and mask uri so we don't see passwords
            uri = URISupport.sanitizeUri(uri);
            table.addRow().addContent(uri,
                    service.getBasePath(),
                    service.getUriTemplate(),
                    service.getMethod(),
                    service.getState());

        }

        table.print(System.out);

        return null;
    }

}
