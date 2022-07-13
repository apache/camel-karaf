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
package org.apache.camel.karaf.commands.internal;

import org.apache.camel.*;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.model.Model;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.model.rest.RestsDefinition;
import org.apache.camel.spi.EndpointRegistry;
import org.apache.camel.spi.ManagementAgent;
import org.apache.camel.spi.RestRegistry;
import org.apache.camel.spi.RuntimeEndpointRegistry;
import org.apache.camel.support.ObjectHelper;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.util.*;

public class CamelController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamelController.class);

    @Reference
    private BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public List<CamelContext> getLocalCamelContexts() {
        List<CamelContext> camelContexts = new ArrayList<>();
        try {
            ServiceReference<?>[] references = bundleContext.getServiceReferences(CamelContext.class.getName(), null);
            if (references != null) {
                for (ServiceReference<?> reference : references) {
                    if (reference != null) {
                        CamelContext camelContext = (CamelContext) bundleContext.getService(reference);
                        if (camelContext != null) {
                            camelContexts.add(camelContext);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot retrieve the list of Camel contexts. This exception is ignored.", e);
        }

        // sort the list
        camelContexts.sort(Comparator.comparing(CamelContext::getName));

        return camelContexts;
    }

    public CamelContext getLocalCamelContext(String name) throws Exception {
        for (CamelContext camelContext : getLocalCamelContexts()) {
            if (camelContext.getName().equals(name)) {
                return camelContext;
            }
        }
        return null;
    }

    public List<Map<String, String>> getCamelContexts() throws Exception {
        List<Map<String, String>> answer = new ArrayList<>();

        List<CamelContext> camelContexts = getLocalCamelContexts();
        for (CamelContext camelContext : camelContexts) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("name", camelContext.getName());
            row.put("state", camelContext.getStatus().name());
            row.put("uptime", camelContext.getUptime());
            ManagedCamelContext mcc = camelContext.getExtension(ManagedCamelContext.class);
            if (mcc != null && mcc.getManagedCamelContext() != null) {
                row.put("exchangesTotal", "" + mcc.getManagedCamelContext().getExchangesTotal());
                row.put("exchangesInflight", "" + mcc.getManagedCamelContext().getExchangesInflight());
                row.put("exchangesFailed", "" + mcc.getManagedCamelContext().getExchangesFailed());
            } else {
                row.put("exchangesTotal", "0");
                row.put("exchangesInflight", "0");
                row.put("exchangesFailed", "0");
            }
            answer.add(row);
        }

        return answer;
    }

    public List<Map<String, String>> getCamelContexts(String filter) throws Exception {
        List<Map<String, String>> answer = new ArrayList<>();

        List<Map<String, String>> context = getCamelContexts();
        if (filter != null) {
            filter = RegexUtil.wildcardAsRegex(filter);
        } else {
            filter = "*";
        }
        for (Map<String, String> entry : context) {
            String name = entry.get("name");
            if (name.equalsIgnoreCase(filter) || MatchUtil.matchWildcard(name, filter) || name.matches(filter)) {
                answer.add(entry);
            }
        }

        return answer;
    }

    public void startContext(String camelContextName) throws Exception {
        final CamelContext context = getLocalCamelContext(camelContextName);
        if (context != null) {
            ObjectHelper.callWithTCCL(() -> {
               context.start();
               return null;
            }, getClassLoader(context));
        }
    }

    public void stopContext(String camelContextName) throws Exception {
        final CamelContext context = getLocalCamelContext(camelContextName);
        if (context != null) {
            ObjectHelper.callWithTCCL(() -> {
               context.stop();
               return null;
            }, getClassLoader(context));
        }
    }

    public void suspendContext(String camelContextName) throws Exception {
        final CamelContext context = getLocalCamelContext(camelContextName);
        if (context != null) {
            ObjectHelper.callWithTCCL(() -> {
                context.suspend();
                return null;
            }, getClassLoader(context));
        }
    }

    public void resumeContext(String camelContextName) throws Exception {
        final CamelContext context = getLocalCamelContext(camelContextName);
        if (context != null) {
            ObjectHelper.callWithTCCL(() -> {
                context.resume();
                return null;
            }, getClassLoader(context));
        }
    }

    public List<Map<String, String>> getRoutes(String camelContextName) throws Exception {
        return getRoutes(camelContextName, null);
    }

    public List<Map<String, String>> getRoutes(String camelContextName, String filter) throws Exception {
        List<Map<String, String>> answer = new ArrayList<>();

        if (camelContextName != null) {
            CamelContext context = getLocalCamelContext(camelContextName);
            if (context != null) {
                for (Route route : context.getRoutes()) {
                    if (filter == null || route.getId().matches(filter)) {
                        Map<String, String> row = new LinkedHashMap<>();
                        row.put("camelContextName", context.getName());
                        row.put("routeId", route.getId());
                        row.put("state", getRouteState(route));
                        row.put("uptime", route.getUptime());
                        ManagedCamelContext mcc = context.getExtension(ManagedCamelContext.class);
                        if (mcc != null && mcc.getManagedCamelContext() != null) {
                            ManagedRouteMBean mr = mcc.getManagedRoute(route.getId());
                            row.put("exchangesTotal", "" + mr.getExchangesTotal());
                            row.put("exchangesInflight", "" + mr.getExchangesInflight());
                            row.put("exchangesFailed", "" + mr.getExchangesFailed());
                        } else {
                            row.put("exchangesTotal", "0");
                            row.put("exchangesInflight", "0");
                            row.put("exchangesFailed", "0");
                        }
                        answer.add(row);
                    }
                }
            }
        } else {
            List<Map<String, String>> camelContexts = this.getCamelContexts();
            for (Map<String, String> row : camelContexts) {
                List<Map<String, String>> routes = getRoutes(row.get("name"), filter);
                answer.addAll(routes);
            }
        }

        // sort the list
        Collections.sort(answer, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> o1, Map<String, String> o2) {
                // group by Camel context first, then by route name
                String c1 = o1.get("camelContextName");
                String c2 = o2.get("camelContextName");

                int answer = c1.compareTo(c2);
                if (answer == 0) {
                    // ok from same camel context, then sort by route id
                    answer = o1.get("routeId").compareTo(o2.get("routeId"));
                }
                return answer;
            }
        });
        return answer;
    }

    public void startRoute(String camelContextName, final String routeId) throws Exception {
        final CamelContext context = getLocalCamelContext(camelContextName);
        if (context != null) {
            ObjectHelper.callWithTCCL(() -> {
                context.getRouteController().startRoute(routeId);
                return null;
            }, getClassLoader(context));
        }
    }

    public void resumeRoute(String camelContextName, final String routeId) throws Exception {
        final CamelContext context = getLocalCamelContext(camelContextName);
        if (context != null) {
            ObjectHelper.callWithTCCL(() -> {
                context.getRouteController().resumeRoute(routeId);
                return null;
            }, getClassLoader(context));
        }
    }

    public List<Map<String, String>> getEndpoints(String camelContextName) throws Exception {
        List<Map<String, String>> answer = new ArrayList<>();

        if (camelContextName != null) {
            CamelContext context = this.getLocalCamelContext(camelContextName);
            if (context != null) {
                List<Endpoint> endpoints = new ArrayList<>(context.getEndpoints());
                // sort routes
                Collections.sort(endpoints, new Comparator<Endpoint>() {
                    @Override
                    public int compare(Endpoint o1, Endpoint o2) {
                        return o1.getEndpointKey().compareTo(o2.getEndpointKey());
                    }
                });
                for (Endpoint endpoint : endpoints) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("camelContextName", context.getName());
                    row.put("uri", endpoint.getEndpointUri());
                    row.put("state", getEndpointState(endpoint));
                    answer.add(row);
                }
            }
        }
        return answer;
    }

    public List<Map<String, String>> getEndpointRuntimeStatistics(String camelContextName) throws Exception {
        List<Map<String, String>> answer = new ArrayList<>();

        if (camelContextName != null) {
            CamelContext context = this.getLocalCamelContext(camelContextName);
            if (context != null && context.getRuntimeEndpointRegistry() != null) {
                EndpointRegistry staticRegistry = context.getEndpointRegistry();
                for (RuntimeEndpointRegistry.Statistic stat : context.getRuntimeEndpointRegistry().getEndpointStatistics()) {

                    String url = stat.getUri();
                    String routeId = stat.getRouteId();
                    String direction = stat.getDirection();
                    boolean isStatic = staticRegistry.isStatic(url);
                    boolean isDynamic = staticRegistry.isDynamic(url);
                    long hits = stat.getHits();

                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("camelContextName", context.getName());
                    row.put("uri", url);
                    row.put("routeId", routeId);
                    row.put("direction", direction);
                    row.put("static", Boolean.toString(isStatic));
                    row.put("dynamic", Boolean.toString(isDynamic));
                    row.put("hits", "" + hits);
                    answer.add(row);
                }
            }

            // sort the list
            Collections.sort(answer, new Comparator<Map<String, String>>() {
                @Override
                public int compare(Map<String, String> endpoint1, Map<String, String> endpoint2) {
                    // sort by route id
                    String route1 = endpoint1.get("routeId");
                    String route2 = endpoint2.get("routeId");
                    int num = route1.compareTo(route2);
                    if (num == 0) {
                        // we want in before out
                        String dir1 = endpoint1.get("direction");
                        String dir2 = endpoint2.get("direction");
                        num = dir1.compareTo(dir2);
                    }
                    return num;
                }

            });
        }
        return answer;
    }

    private static String getEndpointState(Endpoint endpoint) {
        // must use String type to be sure remote JMX can read the attribute without requiring Camel classes.
        if (endpoint instanceof StatefulService) {
            ServiceStatus status = ((StatefulService) endpoint).getStatus();
            return status.name();
        }

        // assume started if not a ServiceSupport instance
        return ServiceStatus.Started.name();
    }

    public String getRestApiDocAsJson(String camelContextName) throws Exception {
        CamelContext context = this.getLocalCamelContext(camelContextName);
        if (context == null) {
            return null;
        }

        return context.getRestRegistry().apiDocAsJson();
    }

    public List<Map<String, String>> getRestServices(String camelContextName) throws Exception {
        List<Map<String, String>> answer = new ArrayList<>();

        if (camelContextName != null) {
            CamelContext context = this.getLocalCamelContext(camelContextName);
            if (context != null) {
                List<RestRegistry.RestService> services = new ArrayList<>(context.getRestRegistry().listAllRestServices());
                Collections.sort(services, new Comparator<RestRegistry.RestService>() {
                    @Override
                    public int compare(RestRegistry.RestService o1, RestRegistry.RestService o2) {
                        return o1.getUrl().compareTo(o2.getUrl());
                    }
                });
                for (RestRegistry.RestService service : services) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("basePath", service.getBasePath());
                    row.put("baseUrl", service.getBaseUrl());
                    row.put("consumes", service.getConsumes());
                    row.put("description", service.getDescription());
                    row.put("inType", service.getInType());
                    row.put("method", service.getMethod());
                    row.put("outType", service.getOutType());
                    row.put("produces", service.getProduces());
                    row.put("state", service.getState());
                    row.put("uriTemplate", service.getUriTemplate());
                    row.put("url", service.getUrl());
                    answer.add(row);
                }
            }
        }
        return answer;
    }

    public String getRestModelAsXml(String camelContextName) throws Exception {
        CamelContext context = this.getLocalCamelContext(camelContextName);
        if (context == null) {
            return null;
        }

        List<RestDefinition> rests = context.getExtension(Model.class).getRestDefinitions();
        if (rests == null || rests.isEmpty()) {
            return null;
        }
        // use a rests definition to dump the rests
        RestsDefinition def = new RestsDefinition();
        def.setRests(rests);

        ExtendedCamelContext ecc = context.adapt(ExtendedCamelContext.class);
        return ecc.getModelToXMLDumper().dumpModelAsXml(context, def);
    }

    public void resetRouteStats(String camelContextName) throws Exception {
        CamelContext context = this.getLocalCamelContext(camelContextName);
        if (context == null) {
            return;
        }

        ManagementAgent agent = context.getManagementStrategy().getManagementAgent();
        if (agent != null) {
            MBeanServer mBeanServer = agent.getMBeanServer();

            // reset route mbeans
            ObjectName query = ObjectName.getInstance(agent.getMBeanObjectDomainName() + ":type=routes,*");
            Set<ObjectName> set = mBeanServer.queryNames(query, null);
            for (ObjectName routeMBean : set) {
                String camelId = (String) mBeanServer.getAttribute(routeMBean, "CamelId");
                if (camelId != null && camelId.equals(context.getName())) {
                    mBeanServer.invoke(routeMBean, "reset", new Object[]{true}, new String[]{"boolean"});
                }
            }
        }
    }

    public String getRouteModelAsXml(String routeId, String camelContextName) throws Exception {
        CamelContext context = this.getLocalCamelContext(camelContextName);
        if (context == null) {
            return null;
        }
        RouteDefinition route = context.getExtension(Model.class).getRouteDefinition(routeId);
        if (route == null) {
            return null;
        }

        ExtendedCamelContext ecc = context.adapt(ExtendedCamelContext.class);
        return ecc.getModelToXMLDumper().dumpModelAsXml(context, route);
    }

    public void stopRoute(String camelContextName, String routeId) throws Exception {
        CamelContext context = getLocalCamelContext(camelContextName);
        if (context != null) {
            context.getRouteController().stopRoute(routeId);
        }
    }

    public void suspendRoute(String camelContextName, String routeId) throws Exception {
        CamelContext context = getLocalCamelContext(camelContextName);
        if (context != null) {
            context.getRouteController().suspendRoute(routeId);
        }
    }

    public List<Map<String, Object>> browseInflightExchanges(String camelContextName, String route, int limit, boolean sortByLongestDuration) throws Exception {
        CamelContext context = getLocalCamelContext(camelContextName);
        if (context == null) {
            return null;
        }

        List<Map<String, Object>> answer = new ArrayList<>();

        ManagementAgent agent = context.getManagementStrategy().getManagementAgent();
        if (agent != null) {
            MBeanServer mBeanServer = agent.getMBeanServer();
            ObjectName on = new ObjectName(agent.getMBeanObjectDomainName() + ":type=services,name=DefaultInflightRepository,context=" + context.getManagementName());
            if (mBeanServer.isRegistered(on)) {
                TabularData list = (TabularData) mBeanServer.invoke(on, "browse", new Object[]{route, limit, sortByLongestDuration}, new String[]{"java.lang.String", "int", "boolean"});
                Collection<CompositeData> values = (Collection<CompositeData>) list.values();
                for (CompositeData data : values) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    Object exchangeId = data.get("exchangeId");
                    if (exchangeId != null) {
                        row.put("exchangeId", exchangeId);
                    }
                    Object fromRouteId = data.get("fromRouteId");
                    if (fromRouteId != null) {
                        row.put("fromRouteId", fromRouteId);
                    }
                    Object routeId = data.get("routeId");
                    if (routeId != null) {
                        row.put("routeId", routeId);
                    }
                    Object nodeId = data.get("nodeId");
                    if (nodeId != null) {
                        row.put("nodeId", nodeId);
                    }
                    Object elapsed = data.get("elapsed");
                    if (elapsed != null) {
                        row.put("elapsed", elapsed);
                    }
                    Object duration = data.get("duration");
                    if (duration != null) {
                        row.put("duration", duration);
                    }
                    answer.add(row);
                }
            }
        }
        return answer;
    }

    public String safeNull(String s) {
        if (org.apache.camel.util.ObjectHelper.isEmpty(s)) {
            return "";
        } else {
            return s;
        }
    }

    public String safeNull(Object s) {
        if (org.apache.camel.util.ObjectHelper.isEmpty(s)) {
            return "";
        } else {
            return s.toString();
        }
    }

    /**
     * Gets classloader associated to the {@link CamelContext}
     *
     * @param context the {@link CamelContext}
     * @return the associated {@link ClassLoader}
     */
    private ClassLoader getClassLoader(CamelContext context) {
        return context.getApplicationContextClassLoader();
    }

    private static String getRouteState(Route route) {
        // must use String type to be sure remote JMX can read the attribute without requiring Camel classes.
        ServiceStatus status = route.getCamelContext().getRouteController().getRouteStatus(route.getId());
        if (status != null) {
            return status.name();
        }

        // assume started if not a ServiceSupport instance
        return ServiceStatus.Started.name();
    }

}
