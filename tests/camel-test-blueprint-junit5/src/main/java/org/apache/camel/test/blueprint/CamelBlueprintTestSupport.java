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
package org.apache.camel.test.blueprint;

import org.apache.aries.blueprint.compendium.cm.CmNamespaceHandler;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.blueprint.CamelBlueprintConfigAdminPlaceholder;
import org.apache.camel.blueprint.CamelBlueprintHelper;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.builder.xml.XMLConverterHelper;
import org.apache.camel.test.junit5.*;
import org.apache.camel.test.junit5.util.CamelContextTestHelper;
import org.apache.camel.test.junit5.util.ExtensionHelper;
import org.apache.camel.util.KeyValueHolder;
import org.apache.camel.util.StopWatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarFile;

/**
 * Base class for OSGi Blueprint unit tests with Camel
 */
public abstract class CamelBlueprintTestSupport extends AbstractTestSupport
        implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final Logger LOG = LoggerFactory.getLogger(CamelBlueprintTestSupport.class);

    /** Name of a system property that sets camel context creation timeout. */
    public static final String SPROP_CAMEL_CONTEXT_CREATION_TIMEOUT = "org.apache.camel.test.blueprint.camelContextCreationTimeout";

    private static ThreadLocal<BundleContext> threadLocalBundleContext = new ThreadLocal<>();
    private volatile BundleContext bundleContext;
    private final Set<ServiceRegistration<?>> services = new LinkedHashSet<>();

    private final StopWatch watch = new StopWatch();

    @RegisterExtension
    @Order(1)
    public final ContextManagerExtension contextManagerExtension;
    private CamelContextManager contextManager;

    protected CamelBlueprintTestSupport() {
        super(new TestExecutionConfiguration(), new CamelBlueprintContextConfiguration());

        configureTest(testConfigurationBuilder);
        configureContext(camelContextConfiguration);
        contextManagerExtension = new ContextManagerExtension(testConfigurationBuilder, camelContextConfiguration);
    }

    /**
     * Override this method if you don't want CamelBlueprintTestSupport create the test bundle
     * @return includeTestBundle
     * If the return value is true CamelBlueprintTestSupport creates the test bundle which includes blueprint configuration files
     * If the return value is false CamelBlueprintTestSupport won't create the test bundle
     */
    protected boolean includeTestBundle() {
        return true;
    }

    /**
     * <p>Override this method if you want to start Blueprint containers asynchronously using the thread
     * that starts the bundles itself.
     * By default this method returns <code>true</code> which means Blueprint Extender will use thread pool
     * (threads named "<code>Blueprint Extender: N</code>") to startup Blueprint containers.</p>
     * <p>Karaf and Fuse OSGi containers use synchronous startup.</p>
     * <p>Asynchronous startup is more in the <em>spirit</em> of OSGi and usually means that if everything works fine
     * asynchronously, it'll work synchronously as well. This isn't always true otherwise.</p>
     * @return <code>true</code> when blueprint containers are to be started asynchronously, otherwise <code>false</code>.
     */
    protected boolean useAsynchronousBlueprintStartup() {
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected BundleContext createBundleContext() throws Exception {
        System.setProperty("org.apache.aries.blueprint.synchronous", Boolean.toString(!useAsynchronousBlueprintStartup()));

        // load configuration file
        CamelBlueprintConfigAdminPlaceholder[] placeholders = loadConfigAdminConfigurationFile();
        CamelBlueprintConfigAdminPlaceholder[] configAdminPersistenceIdFiles = new CamelBlueprintConfigAdminPlaceholder[0];
        if (placeholders != null) {
            for (CamelBlueprintConfigAdminPlaceholder placeholder : placeholders) {
                String fileName = placeholder.getFilename();
                if (!new File(fileName).exists()) {
                    throw new IllegalArgumentException("The provided file \"" + fileName + "\" from loadConfigAdminConfigurationFile doesn't exist");
                }
            }
            configAdminPersistenceIdFiles = placeholders;
        }

        // fetch initial configadmin configuration if provided programmatically
        Properties initialConfiguration = new Properties();
        String pid = setConfigAdminInitialConfiguration(initialConfiguration);
        if (pid != null) {
            configAdminPersistenceIdFiles = new CamelBlueprintConfigAdminPlaceholder[]{
                    new CamelBlueprintConfigAdminPlaceholder(prepareInitialConfigFile(initialConfiguration), pid)
            };
        }

        final String symbolicName = getClass().getSimpleName();
        final BundleContext answer = CamelBlueprintHelper.createBundleContext(symbolicName, getBlueprintDescriptor(),
                includeTestBundle(), getBundleFilter(), getBundleVersion(), getBundleDirectives(), configAdminPersistenceIdFiles);

        boolean expectReload = expectBlueprintContainerReloadOnConfigAdminUpdate();

        // must register override properties early in OSGi containers
        var extra = useOverridePropertiesWithPropertiesComponent();
        if (extra != null) {
            answer.registerService(PropertiesComponent.OVERRIDE_PROPERTIES, extra, null);
        }

        Map<String, KeyValueHolder<Object, Dictionary>> map = new LinkedHashMap<>();
        addServicesOnStartup(map);

        List<KeyValueHolder<String, KeyValueHolder<Object, Dictionary>>> servicesList = new LinkedList<>();
        for (Map.Entry<String, KeyValueHolder<Object, Dictionary>> entry : map.entrySet()) {
            servicesList.add(asKeyValueService(entry.getKey(), entry.getValue().getKey(), entry.getValue().getValue()));
        }

        addServicesOnStartup(servicesList);

        for (KeyValueHolder<String, KeyValueHolder<Object, Dictionary>> item : servicesList) {
            String clazz = item.getKey();
            Object service = item.getValue().getKey();
            Dictionary dict = item.getValue().getValue();
            LOG.debug("Registering service {} -> {}", clazz, service);
            ServiceRegistration<?> reg = answer.registerService(clazz, service, dict);
            if (reg != null) {
                services.add(reg);
            }
        }

        // if blueprint XML uses <cm:property-placeholder> (any update-strategy and any default properties)
        // - org.apache.aries.blueprint.compendium.cm.ManagedObjectManager.register() is called
        // - ManagedServiceUpdate is scheduled in felix.cm
        // - org.apache.felix.cm.impl.ConfigurationImpl.setDynamicBundleLocation() is called
        // - CM_LOCATION_CHANGED event is fired
        // - if BP was already created, it's <cm:property-placeholder> receives the event and
        // - org.apache.aries.blueprint.compendium.cm.CmPropertyPlaceholder.updated() is called,
        //   but no BP reload occurs
        // we will however wait for BP container of the test bundle to become CREATED for the first time
        // each configadmin update *may* lead to reload of BP container, if it uses <cm:property-placeholder>
        // with update-strategy="reload"

        // we will gather timestamps of BP events. We don't want to be fooled but repeated events related
        // to the same state of BP container
        Set<Long> bpEvents = new HashSet<>();

        CamelBlueprintHelper.waitForBlueprintContainer(bpEvents, answer, symbolicName, BlueprintEvent.CREATED, null);

        // must reuse props as we can do both load from .cfg file and override afterwards
        final Dictionary props = new Properties();

        // allow end user to override properties
        pid = useOverridePropertiesWithConfigAdmin(props);
        if (pid != null) {
            // we will update the configuration again
            ConfigurationAdmin configAdmin = CamelBlueprintHelper.getOsgiService(answer, ConfigurationAdmin.class);
            // passing null as second argument ties the configuration to correct bundle.
            // using single-arg method causes:
            // *ERROR* Cannot use configuration xxx.properties for [org.osgi.service.cm.ManagedService, id=N, bundle=N/jar:file:xyz.jar!/]: No visibility to configuration bound to felix-connect
            final Configuration config = configAdmin.getConfiguration(pid, null);
            if (config == null) {
                throw new IllegalArgumentException("Cannot find configuration with pid " + pid + " in OSGi ConfigurationAdmin service.");
            }
            // lets merge configurations
            Dictionary<String, Object> currentProperties = config.getProperties();
            final Dictionary newProps = new Properties();
            if (currentProperties == null) {
                currentProperties = newProps;
            }
            for (Enumeration<String> ek = currentProperties.keys(); ek.hasMoreElements();) {
                String k = ek.nextElement();
                newProps.put(k, currentProperties.get(k));
            }
            for (String p : ((Properties) props).stringPropertyNames()) {
                newProps.put(p, ((Properties) props).getProperty(p));
            }

            LOG.info("Updating ConfigAdmin {} by overriding properties {}", config, newProps);
            if (expectReload) {
                CamelBlueprintHelper.waitForBlueprintContainer(bpEvents, answer, symbolicName, BlueprintEvent.CREATED, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            config.update(newProps);
                        } catch (IOException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    }
                });
            } else {
                config.update(newProps);
            }
        }

        return answer;
    }

    @BeforeEach
    public void setUp() throws Exception {
        System.setProperty("skipStartingCamelContext", "true");
        System.setProperty("registerBlueprintCamelContextEager", "true");

        if (testConfigurationBuilder.isCreateCamelContextPerClass()) {
            // test is per class, so only setup once (the first time)
            boolean first = threadLocalBundleContext.get() == null;
            if (first) {
                threadLocalBundleContext.set(createBundleContext());
            }
            bundleContext = threadLocalBundleContext.get();
        } else {
            bundleContext = createBundleContext();
        }

        ExtensionHelper.hasUnsupported(getClass());

        setupResources();

        contextManager = contextManagerExtension.getContextManager();
        contextManager.createCamelContext(this);
        context = contextManager.context();



        // only start timing after all the setup
        watch.restart();

        // we don't have to wait for BP container's OSGi service - we've already waited
        // for BlueprintEvent.CREATED

        // start context when we are ready
        LOG.debug("Starting CamelContext: {}", context.getName());
        if (testConfigurationBuilder.isUseAdviceWith()) {
            LOG.info("Skipping starting CamelContext as isUseAdviceWith is set to true.");
        } else {
            context.start();
        }
    }

    /**
     * Override this method to add services to be registered on startup.
     * <p/>
     * You can use the builder methods {@link #asService(Object, Dictionary)}, {@link #asService(Object, String, String)}
     * to make it easy to add the services to the map.
     */
    protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
        // noop
    }

    /**
     * This method may be overriden to instruct BP test support that BP container will reloaded when
     * Config Admin configuration is updated. By default, this is expected, when blueprint XML definition
     * contains <code>&lt;cm:property-placeholder persistent-id="PID" update-strategy="reload"&gt;</code>
     */
    protected boolean expectBlueprintContainerReloadOnConfigAdminUpdate() {
        boolean expectedReload = false;
        DocumentBuilderFactory dbf = new XMLConverterHelper().createDocumentBuilderFactory();
        try {
            // cm-1.0 doesn't define update-strategy attribute
            Set<String> cmNamesaces = new HashSet<>(Arrays.asList(
                    CmNamespaceHandler.BLUEPRINT_CM_NAMESPACE_1_1,
                    CmNamespaceHandler.BLUEPRINT_CM_NAMESPACE_1_2,
                    CmNamespaceHandler.BLUEPRINT_CM_NAMESPACE_1_3
            ));
            for (URL descriptor : CamelBlueprintHelper.getBlueprintDescriptors(getBlueprintDescriptor())) {
                DocumentBuilder db = dbf.newDocumentBuilder();
                try (InputStream is = descriptor.openStream()) {
                    Document doc = db.parse(is);
                    NodeList nl = doc.getDocumentElement().getChildNodes();
                    for (int i = 0; i < nl.getLength(); i++) {
                        Node node = nl.item(i);
                        if (node instanceof Element pp && cmNamesaces.contains(pp.getNamespaceURI())) {
                            String us = pp.getAttribute("update-strategy");
                            if (us.equals("reload")) {
                                expectedReload = true;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return expectedReload;
    }

    /**
     * Override this method to add services to be registered on startup.
     * <p/>
     * You can use the builder methods {@link #asKeyValueService(String, Object, Dictionary)}
     * to make it easy to add the services to the List.
     */
    protected void addServicesOnStartup(List<KeyValueHolder<String, KeyValueHolder<Object, Dictionary>>> services) {
        // noop
    }

    /**
     * Creates a holder for the given service, which make it easier to use {@link #addServicesOnStartup(Map)}
     */
    protected KeyValueHolder<Object, Dictionary> asService(Object service, Dictionary dict) {
        return new KeyValueHolder<>(service, dict);
    }

    /**
     * Creates a holder for the given service, which make it easier to use {@link #addServicesOnStartup(List)}
     */
    protected KeyValueHolder<String, KeyValueHolder<Object, Dictionary>> asKeyValueService(String name, Object service, Dictionary dict) {
        return new KeyValueHolder<>(name, new KeyValueHolder<>(service, dict));
    }

    /**
     * Creates a holder for the given service, which make it easier to use {@link #addServicesOnStartup(Map)}
     */
    protected KeyValueHolder<Object, Dictionary> asService(Object service, String key, String value) {
        Properties prop = new Properties();
        if (key != null && value != null) {
            prop.put(key, value);
        }
        return new KeyValueHolder<>(service, prop);
    }

    /**
     * <p>Override this method to override config admin properties. Overriden properties will be passed to
     * {@link Configuration#update(Dictionary)} and may or may not lead to reload of Blueprint container - this
     * depends on <code>update-strategy="reload|none"</code> in <code>&lt;cm:property-placeholder&gt;</code></p>
     * <p>This method should be used to simulate configuration update <strong>after</strong> Blueprint container
     * is already initialized and started. Don't use this method to initialized ConfigAdmin configuration.</p>
     *
     * @param props properties where you add the properties to override
     * @return the PID of the OSGi {@link ConfigurationAdmin} which are defined in the Blueprint XML file.
     */
    protected String useOverridePropertiesWithConfigAdmin(Dictionary<String, String> props) throws Exception {
        return null;
    }

    /**
     * Override this method and provide the name of the .cfg configuration file to use for
     * ConfigAdmin service. Provided file will be used to initialize ConfigAdmin configuration before Blueprint
     * container is loaded.
     *
     * @return the name of the path for the .cfg file to load, and the persistence-id of the property placeholder.
     */
    protected CamelBlueprintConfigAdminPlaceholder[] loadConfigAdminConfigurationFile() {
        return null;
    }

    /**
     * Override this method as an alternative to {@link #loadConfigAdminConfigurationFile()} if there's a need
     * to set initial ConfigAdmin configuration without using files.
     *
     * @param props always non-null. Tests may initialize ConfigAdmin configuration by returning PID.
     * @return persistence-id of the property placeholder. If non-null, <code>props</code> will be used as
     * initial ConfigAdmin configuration
     */
    protected String setConfigAdminInitialConfiguration(Properties props) {
        return null;
    }

    @AfterEach
    public void afterEach() throws Exception {
        System.clearProperty("skipStartingCamelContext");
        System.clearProperty("registerBlueprintCamelContextEager");

        tearDown(new TestInfo() {
            @Override
            public String getDisplayName() {
                return "";
            }

            @Override
            public Set<String> getTags() {
                return Set.of();
            }

            @Override
            public Optional<Class<?>> getTestClass() {
                return Optional.empty();
            }

            @Override
            public Optional<Method> getTestMethod() {
                return Optional.empty();
            }
        });

        // unregister services
        if (bundleContext != null) {
            for (ServiceRegistration<?> reg : services) {
                bundleContext.ungetService(reg.getReference());
            }
        }

        // close bundle context
        if (bundleContext != null) {
            // remember bundles before closing
            Bundle[] bundles = bundleContext.getBundles();
            // close bundle context
            CamelBlueprintHelper.disposeBundleContext(bundleContext);
            // now close jar files from the bundles
            closeBundleJArFile(bundles);
        }
    }

    public final void tearDown(TestInfo testInfo) throws Exception {
        long time = watch.taken();
        LOG.debug("tearDown()");

        if (contextManager != null) {
            contextManager.dumpRouteCoverage(getClass(), testInfo.getDisplayName(), time);
            String dump = CamelContextTestHelper.getRouteDump(getDumpRoute());
            contextManager.dumpRoute(getClass(), testInfo.getDisplayName(), dump);
        } else {
            LOG.warn(
                    "A context manager is required to dump the route coverage for the Camel context but it's not available (it's null). "
                            + "It's likely that the test is misconfigured!");
        }
        cleanupResources();
    }

    @Override
    public void cleanupResources() throws Exception {
        if (threadLocalBundleContext.get() != null) {
            CamelBlueprintHelper.disposeBundleContext(threadLocalBundleContext.get());
            threadLocalBundleContext.remove();
        }
        super.cleanupResources();
    }

    /**
     * Felix Connect leaks "open files" as a JarFile on Bundle Revision is not closed when stopping the bundle
     * which can cause the JVM to open up too many file handles.
     */
    private void closeBundleJArFile(Bundle[] bundles) {
        for (Bundle bundle : bundles) {
            try {
                // not all bundles is from PojoSRBundle that has a revision
                Field field = bundle.getClass().getDeclaredField("m_revision");
                field.setAccessible(true);
                Object val = field.get(bundle);
                field = val.getClass().getDeclaredField("m_jar");
                field.setAccessible(true);
                Object mJar = field.get(val);
                if (mJar instanceof JarFile jf) {
                    LOG.debug("Closing bundle[{}] JarFile: {}", bundle.getBundleId(), jf.getName());
                    jf.close();
                    LOG.trace("Closed bundle[{}] JarFile: {}", bundle.getBundleId(), jf.getName());
                }
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    /**
     * Return the system bundle context
     */
    protected BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * Gets the bundle descriptor from the classpath.
     * <p/>
     * Return the location(s) of the bundle descriptors from the classpath.
     * Separate multiple locations by comma, or return a single location.
     * <p/>
     * Only one CamelContext is supported per blueprint bundle,
     * so if you have multiple XML files then only one of them should have <tt>&lt;camelContext&gt</tt>.
     * <p/>
     * For example override this method and return <tt>OSGI-INF/blueprint/camel-context.xml</tt>
     *
     * @return the location of the bundle descriptor file.
     */
    protected String getBlueprintDescriptor() {
        return null;
    }

    /**
     * Gets filter expression of bundle descriptors.
     * Modify this method if you wish to change default behavior.
     *
     * @return filter expression for OSGi bundles.
     */
    protected String getBundleFilter() {
        return CamelBlueprintHelper.BUNDLE_FILTER;
    }

    /**
     * Gets test bundle version.
     * Modify this method if you wish to change default behavior.
     *
     * @return test bundle version
     */
    protected String getBundleVersion() {
        return CamelBlueprintHelper.BUNDLE_VERSION;
    }

    /**
     * Gets the bundle directives.
     * <p/>
     * Modify this method if you wish to add some directives.
     */
    protected String getBundleDirectives() {
        return null;
    }

    /**
     * Returns how long to wait for Camel Context
     * to be created.
     *
     * @return timeout in milliseconds.
     */
    protected Long getCamelContextCreationTimeout() {
        String tm = System.getProperty(SPROP_CAMEL_CONTEXT_CREATION_TIMEOUT);
        if (tm == null) {
            return null;
        }
        try {
            Long val = Long.valueOf(tm);
            if (val < 0) {
                throw new IllegalArgumentException("Value of "
                        + SPROP_CAMEL_CONTEXT_CREATION_TIMEOUT
                        + " cannot be negative.");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value of "
                    + SPROP_CAMEL_CONTEXT_CREATION_TIMEOUT
                    + " has wrong format.", e);
        }
    }

    /**
     * Gets filter expression for the Camel context you want to test.
     * Modify this if you have multiple contexts in the OSGi registry and want to test a specific one.
     *
     * @return filter expression for Camel context.
     */
    protected String getCamelContextFilter() {
        return null;
    }

    protected CamelContext createCamelContext() throws Exception {
        CamelContext answer;
        Long timeout = getCamelContextCreationTimeout();
        if (timeout == null) {
            answer = CamelBlueprintHelper.getOsgiService(bundleContext, CamelContext.class, getCamelContextFilter());
        } else if (timeout >= 0) {
            answer = CamelBlueprintHelper.getOsgiService(bundleContext, CamelContext.class, getCamelContextFilter(), timeout);
        } else {
            throw new IllegalArgumentException("getCamelContextCreationTimeout cannot return a negative value.");
        }
        // must override context so we use the correct one in testing
        context = (ModelCamelContext) answer;
        return answer;
    }


    protected <T> T getOsgiService(Class<T> type) {
        return CamelBlueprintHelper.getOsgiService(bundleContext, type);
    }

    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return CamelBlueprintHelper.getOsgiService(bundleContext, type, timeout);
    }

    protected <T> T getOsgiService(Class<T> type, String filter) {
        return CamelBlueprintHelper.getOsgiService(bundleContext, type, filter);
    }

    protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        return CamelBlueprintHelper.getOsgiService(bundleContext, type, filter, timeout);
    }

    /**
     * Create a temporary File with persisted configuration for ConfigAdmin
     */
    private String prepareInitialConfigFile(Properties initialConfiguration) throws IOException {
        File dir = new File("target/etc");
        dir.mkdirs();
        File cfg = Files.createTempFile(dir.toPath(), "properties-", ".cfg").toFile();
        try (FileWriter writer = new FileWriter(cfg)) {
            initialConfiguration.store(writer, null);
        }
        return cfg.getAbsolutePath();
    }

    /**
     * Resolves the mandatory Mock endpoint using a URI of the form <code>mock:someName</code>
     *
     * @param  uri the URI which typically starts with "mock:" and has some name
     * @return     the mandatory mock endpoint or an exception is thrown if it could not be resolved
     */
    protected final MockEndpoint getMockEndpoint(String uri) {
        return TestSupport.getMockEndpoint(context, uri, true);
    }

    @Override
    public void configureContext(CamelContextConfiguration camelContextConfiguration) {
        if (camelContextConfiguration instanceof CamelBlueprintContextConfiguration camelBlueprintContextConfiguration) {
            camelBlueprintContextConfiguration
                    .withBlueprintCamelContextSupplier(this::createCamelContext)
                    .withBlueprintPostProcessor(this::postProcessTest)
                    .withBlueprintRoutesSupplier(this::createRouteBuilders)
                    .withRegistryBinder(this::bindToRegistry)
                    .withUseOverridePropertiesWithPropertiesComponent(useOverridePropertiesWithPropertiesComponent())
                    .withRouteFilterExcludePattern(getRouteFilterExcludePattern())
                    .withRouteFilterIncludePattern(getRouteFilterIncludePattern())
                    .withMockEndpoints(isMockEndpoints())
                    .withMockEndpointsAndSkip(isMockEndpointsAndSkip())
                    .withBreakpoint(createDebugBreakpoint());
        } else {
            throw new IllegalArgumentException("camelContextConfiguration is not of type CamelBlueprintContextConfiguration");
        }
    }

    @Override
    public void configureTest(TestExecutionConfiguration testExecutionConfiguration) {
        boolean coverage = CamelContextTestHelper.isRouteCoverageEnabled(isDumpRouteCoverage());
        String dump = CamelContextTestHelper.getRouteDump(getDumpRoute());
        boolean jmx = coverage || useJmx(); // route coverage requires JMX

        testExecutionConfiguration.withJMX(jmx)
                .withUseRouteBuilder(isUseRouteBuilder())
                .withUseAdviceWith(isUseAdviceWith())
                .withDumpRouteCoverage(coverage)
                .withDumpRoute(dump);
    }

    /**
     * Allows binding custom beans to the Camel {@link Registry}.
     */
    protected void bindToRegistry(Registry registry) throws Exception {
        // noop
    }

    /**
     * Factory method which derived classes can use to create an array of {@link RouteBuilder}s
     * to define the routes for testing.
     *
     * @see #createRouteBuilder()
     */
    protected RoutesBuilder[] createRouteBuilders() throws Exception {
        return new RoutesBuilder[] { createRouteBuilder() };
    }



    /**
     * Factory method which derived classes can use to create a {@link RouteBuilder} to define the routes for testing
     *
     * @see #createRouteBuilders()
     */
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() {
                // no routes added by default
            }
        };
    }

    protected void postProcessTest() throws Exception {
        context = contextManager.context();
        template = contextManager.template();
        fluentTemplate = contextManager.fluentTemplate();
        consumer = contextManager.consumer();
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        LOG.trace("Before test execution {}", context.getDisplayName());
        watch.restart();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        watch.taken();
    }

    /**
     * Method to overide to create a {@link DebugBreakpoint}
     *
     * @see #createRouteBuilders()
     */
    protected DebugBreakpoint createDebugBreakpoint() {
        // No Debug Breakpoint by default
        return null;
    }
}
