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
import org.apache.camel.spi.EndpointRegistry;
import org.apache.camel.spi.RuntimeEndpointRegistry;
import org.apache.camel.util.URISupport;
import org.apache.karaf.shell.api.action.*;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.net.URLDecoder;
import java.util.List;

@Command(scope = "camel", name = "endpoint-stats", description = "List the statistics of the Camel endpoints")
@Service
public class EndpointStats extends CamelCommandSupport implements Action {

    @Argument(index = 0, name = "context", description = "The name of the Camel context (support wildcard)", required = false, multiValued = false)
    @Completion(CamelContextCompleter.class)
    String name;

    @Option(name = "--filter", aliases = "-f", description = "Filter the list by in, out, static, dynamic", required = false, multiValued = true)
    String[] filter;

    @Option(name = "--decode", aliases = "-d", description = "Whether to decode the endpoint uri so its human readable", required = false, multiValued = false, valueToShowInHelp = "true")
    boolean decode = true;

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("Context");
        table.column("Uri");
        table.column("Route Id");
        table.column("Direction");
        table.column("Static");
        table.column("Dynamic");
        table.column("Total #");

        List<CamelContext> camelContexts = getCamelContext(name);

        for (CamelContext camelContext : camelContexts) {
            if (camelContext.getRuntimeEndpointRegistry() != null) {
                EndpointRegistry endpointRegistry = camelContext.getEndpointRegistry();
                for (RuntimeEndpointRegistry.Statistic stat : camelContext.getRuntimeEndpointRegistry().getEndpointStatistics()) {
                    String uri = stat.getUri();
                    String routeId = stat.getRouteId();
                    String direction = stat.getDirection();
                    boolean isStatic = endpointRegistry.isStatic(uri);
                    boolean isDynamic = endpointRegistry.isDynamic(uri);
                    long hits = stat.getHits();

                    if (decode) {
                        // decode uri so it's more human readable
                        uri = URLDecoder.decode(uri, "UTF-8");
                    }
                    // sanitize and mask uri so we don't see passwords
                    uri = URISupport.sanitizeUri(uri);

                    // should we filter ?
                    if (isValidRow(direction, Boolean.toString(isStatic), Boolean.toString(isDynamic))) {
                        table.addRow().addContent(camelContext.getName(),
                                uri,
                                routeId,
                                direction,
                                isStatic,
                                isDynamic,
                                hits);
                    }

                }
            }
        }

        table.print(System.out);
        return null;
    }

    private boolean isValidRow(String direction, String isStatic, String isDynamic) {
        if (filter == null || filter.length == 0) {
            return true;
        }

        boolean answer = false;
        for (String f : filter) {
            if ("in".equals(f)) {
                answer = "in".equals(direction);
            } else if ("out".equals(f)) {
                answer = "out".equals(direction);
            } else if ("static".equals(f)) {
                answer = "true".equals(isStatic);
            } else if ("dynamic".equals(f)) {
                answer = "true".equals(isDynamic);
            }
            // all filters must apply to accept when multi valued
            if (!answer) {
                return false;
            }
        }

        return answer;
    }

}
