/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.camel.itests;


import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.blueprint.BlueprintCamelContext;
import org.apache.camel.karaf.core.OsgiDefaultCamelContext;
import org.apache.karaf.itests.KarafTestSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.remote.RBCRemoteTargetOptions;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFilePutOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.camel.itests.CamelKarafTestHint.DEFAULT_TIMEOUT;
import static org.ops4j.pax.exam.OptionUtils.combine;

@ExamFactory(TestContainerFactoryFailureAware.class)
public abstract class AbstractCamelRouteITest extends KarafTestSupport implements CamelContextProvider {

    public static final int CAMEL_KARAF_INTEGRATION_TEST_DEBUG_DEFAULT_PORT = 8889;
    public static final String CAMEL_KARAF_INTEGRATION_TEST_DEBUG_PROPERTY = "camel.karaf.itest.debug";
    static final int CAMEL_KARAF_INTEGRATION_TEST_CONTEXT_FINDER_RETRY_DEFAULT = 3;
    static final int CAMEL_KARAF_INTEGRATION_TEST_CONTEXT_FINDER_RETRY_INTERVAL_DEFAULT = 5;
    static final String CAMEL_KARAF_INTEGRATION_TEST_CONTEXT_FINDER_RETRY_PROPERTY = "camel.karaf.itest.context.finder.retry";
    static final String CAMEL_KARAF_INTEGRATION_TEST_CONTEXT_FINDER_RETRY_INTERVAL_PROPERTY = "camel.karaf.itest.context.finder.retry.interval";
    static final String CAMEL_KARAF_INTEGRATION_TEST_ROUTE_SUPPLIERS_PROPERTY = "camel.karaf.itest.route.suppliers";
    static final String CAMEL_KARAF_INTEGRATION_TEST_IGNORE_ROUTE_SUPPLIERS_PROPERTY = "camel.karaf.itest.ignore.route.suppliers";
    static final String CAMEL_KARAF_INTEGRATION_TEST_DUMP_LOGS_PROPERTY = "camel.karaf.itest.dump.logs";

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCamelRouteITest.class);
    private final Map<CamelContextKey, CamelContext> contexts = new ConcurrentHashMap<>();
    private final Map<CamelContextKey, ProducerTemplate> templates = new ConcurrentHashMap<>();
    @Rule
    public final DumpFileOnError dumpFileOnError;
    private List<String> requiredBundles;

    protected AbstractCamelRouteITest() {
        this.retry = new Retry(retryOnFailure());
        this.dumpFileOnError = new DumpFileOnError(
            getKarafLogFile(), System.err, Boolean.getBoolean(CAMEL_KARAF_INTEGRATION_TEST_DUMP_LOGS_PROPERTY)
        );
    }

    public String getCamelKarafVersion() {
        String version = System.getProperty("camel.karaf.version");
        if (version == null) {
            version = Utils.loadCamelKarafVersion();
        }
        return version;
    }

    public String getBaseDir() {
        String location = System.getProperty("project.target");
        if (location == null) {
            throw new IllegalStateException("The system property 'project.target' must be set to the target directory of" +
                    " the project or the method getBaseDir must be overridden to provide the base directory");
        }
        return location;
    }

    public File getUsersFile() {
        // retrieve the users.properties file from the resources folder to avoid file duplication
        String location = System.getProperty("users.file.location");
        if (location == null) {
            location = Utils.loadUsersFileIfAbsent(getBaseDir());
        }
        return new File(location);
    }

    @Override
    public File getConfigFile(String path) {
        if (path.equals("/etc/users.properties")) {
            return getUsersFile();
        }
        return super.getConfigFile(path);
    }

    @Configuration
    @Override
    public Option[] config() {
        String camelKarafVersion = getCamelKarafVersion();
        if (camelKarafVersion == null) {
            throw new IllegalArgumentException("The system property 'camel.karaf.version' must be set or the method " +
                    "getCamelKarafVersion must be overridden to provide the version of Camel Karaf to use");
        }
        Option[] options = new Option[]{
            CoreOptions.systemTimeout(timeoutInMillis()),
            RBCRemoteTargetOptions.waitForRBCFor((int) timeoutInMillis()),
            CoreOptions.systemProperty(CAMEL_KARAF_INTEGRATION_TEST_DUMP_LOGS_PROPERTY).value(System.getProperty(CAMEL_KARAF_INTEGRATION_TEST_DUMP_LOGS_PROPERTY, "false")),
            CoreOptions.systemProperty("project.target").value(getBaseDir()),
            KarafDistributionOption.features("mvn:org.apache.camel.karaf/apache-camel/%s/xml/features".formatted(camelKarafVersion), "scr", getMode().getFeatureName()),
            CoreOptions.mavenBundle().groupId("org.apache.camel.karaf").artifactId("camel-integration-test").version(camelKarafVersion)
        };
        Option[] combine = combine(updatePorts(super.config()), options);
        if (isDebugModeEnabled()) {
            combine = combine(combine, KarafDistributionOption.debugConfiguration(Integer.toString(getDebugPort()), true));
        }
        if (hasExternalResources()) {
            combine = combine(combine, getExternalResourceOptions());
        }
        if (ignoreCamelRouteSuppliers()) {
            combine = combine(combine, getIgnoreCamelRouteSupplier());
        } else if (hasCamelRouteSupplierFilter()) {
            combine = combine(combine, getCamelRouteSupplierFilter());
        }
        return combine(combine, getAdditionalOptions());
    }

    /**
     * @return the location of the Karaf log file.
     */
    private static @NotNull File getKarafLogFile() {
        return new File(System.getProperty("karaf.log"), "karaf.log");
    }

    /**
     * Indicates whether the debug mode is enabled or not. The debug mode is enabled when the system property
     * {@link #CAMEL_KARAF_INTEGRATION_TEST_DEBUG_PROPERTY} is set.
     * @return {@code true} if the debug mode is enabled, {@code false} otherwise
     */
    private static boolean isDebugModeEnabled() {
        return System.getProperty(CAMEL_KARAF_INTEGRATION_TEST_DEBUG_PROPERTY) != null;
    }

    /**
     * Returns the debug port to use when the debug mode is enabled, corresponding to the value of the system property
     * {@link #CAMEL_KARAF_INTEGRATION_TEST_DEBUG_PROPERTY}. The default value is {@link #CAMEL_KARAF_INTEGRATION_TEST_DEBUG_DEFAULT_PORT}.
     * @return the debug port
     */
    private static int getDebugPort() {
        return Integer.getInteger(CAMEL_KARAF_INTEGRATION_TEST_DEBUG_PROPERTY, CAMEL_KARAF_INTEGRATION_TEST_DEBUG_DEFAULT_PORT);
    }

    /**
     * Returns the number of retries to perform when trying to find a Camel context, corresponding to the value of the
     * system property {@link #CAMEL_KARAF_INTEGRATION_TEST_CONTEXT_FINDER_RETRY_PROPERTY}. The default value is
     * {@link #CAMEL_KARAF_INTEGRATION_TEST_CONTEXT_FINDER_RETRY_DEFAULT}.
     * @return the number of retries
     */
    private static int getContextFinderRetry() {
        return Integer.getInteger(
            CAMEL_KARAF_INTEGRATION_TEST_CONTEXT_FINDER_RETRY_PROPERTY,
            CAMEL_KARAF_INTEGRATION_TEST_CONTEXT_FINDER_RETRY_DEFAULT
        );
    }

    /**
     * Returns the interval in seconds between each retry when trying to find a Camel context, corresponding to the value of the
     * system property {@link #CAMEL_KARAF_INTEGRATION_TEST_CONTEXT_FINDER_RETRY_INTERVAL_PROPERTY}. The default value is
     * {@link #CAMEL_KARAF_INTEGRATION_TEST_CONTEXT_FINDER_RETRY_INTERVAL_DEFAULT}.
     * @return the interval in seconds between each retry when trying to find a Camel context
     */
    private static int getContextFinderRetryInterval() {
        return Integer.getInteger(
            CAMEL_KARAF_INTEGRATION_TEST_CONTEXT_FINDER_RETRY_INTERVAL_PROPERTY,
            CAMEL_KARAF_INTEGRATION_TEST_CONTEXT_FINDER_RETRY_INTERVAL_DEFAULT
        );
    }

    /**
     * Update the ports in the given options to work around the issue with the default method {@code getAvailablePort}
     * that doesn't seem to work properly on macOS because the ports are not modified when a Karaf instance is already
     * started locally.
     */
    private static Option[] updatePorts(Option[] options) {
        for (int i = 0; i < options.length; i++) {
            Option option = options[i];
            if (option instanceof KarafDistributionConfigurationFilePutOption putOption) {
                if (putOption.getConfigurationFilePath().equals("etc/org.ops4j.pax.web.cfg") && putOption.getKey().equals("org.osgi.service.http.port")) {
                    String httpPort = Integer.toString(Utils.getAvailablePort(Integer.parseInt(MIN_HTTP_PORT), Integer.parseInt(MAX_HTTP_PORT)));
                    options[i] = KarafDistributionOption.editConfigurationFilePut(putOption.getConfigurationFilePath(), putOption.getKey(), httpPort);
                } else if (putOption.getConfigurationFilePath().equals("etc/org.apache.karaf.management.cfg")) {
                    if (putOption.getKey().equals("rmiRegistryPort")) {
                        String rmiRegistryPort = Integer.toString(Utils.getAvailablePort(Integer.parseInt(MIN_RMI_REG_PORT), Integer.parseInt(MAX_RMI_REG_PORT)));
                        options[i] = KarafDistributionOption.editConfigurationFilePut(putOption.getConfigurationFilePath(), putOption.getKey(), rmiRegistryPort);
                    } else if (putOption.getKey().equals("rmiServerPort")) {
                        String rmiServerPort = Integer.toString(Utils.getAvailablePort(Integer.parseInt(MIN_RMI_SERVER_PORT), Integer.parseInt(MAX_RMI_SERVER_PORT)));
                        options[i] = KarafDistributionOption.editConfigurationFilePut(putOption.getConfigurationFilePath(), putOption.getKey(), rmiServerPort);
                    }
                } else if (putOption.getConfigurationFilePath().equals("etc/org.apache.karaf.shell.cfg") && putOption.getKey().equals("sshPort")) {
                    String sshPort = Integer.toString(Utils.getAvailablePort(Integer.parseInt(MIN_SSH_PORT), Integer.parseInt(MAX_SSH_PORT)));
                    options[i] = KarafDistributionOption.editConfigurationFilePut(putOption.getConfigurationFilePath(), putOption.getKey(), sshPort);
                }
            }
        }
        return options;
    }

    /**
     * @return the options provided by the external resources.
     */
    @NotNull
    private static Option[] getExternalResourceOptions() {
        return PaxExamWithExternalResource.systemProperties().entrySet().stream()
                .map(e -> CoreOptions.systemProperty(e.getKey()).value(e.getValue()))
                .toArray(Option[]::new);
    }

    /**
     * Returns the timeout in milliseconds for the test, corresponding to the value of the
     * {@link CamelKarafTestHint#timeout()} annotation multiplied by one thousand.
     * @return the timeout in milliseconds for the test
     */
    private long timeoutInMillis() {
        return TimeUnit.SECONDS.toMillis(getCamelKarafTestHint().map(CamelKarafTestHint::timeout).orElse(DEFAULT_TIMEOUT));
    }

    /**
     * Indicates whether the test should be retried on failure. By default, no retry is performed.
     * @return {@code true} if the test should be retried on failure, {@code false} otherwise
     */
    private boolean retryOnFailure() {
        return getCamelKarafTestHint().filter(CamelKarafTestHint::retryOnFailure).isPresent();
    }

    /**
     * Indicates whether the test requires external resources or not. By default, no external resources are required.
     * @return {@code true} if the test requires external resources, {@code false} otherwise
     */
    private boolean hasExternalResources() {
        return getCamelKarafTestHint().filter(hint -> hint.externalResourceProvider() != Object.class).isPresent();
    }

    /**
     * @return the {@link CamelKarafTestHint} annotation of the test class
     */
    private Optional<CamelKarafTestHint> getCamelKarafTestHint() {
        return getCamelKarafTestHint(getClass());
    }

    /**
     * @return the {@link CamelKarafTestHint} annotation of the given test class
     */
    private static Optional<CamelKarafTestHint> getCamelKarafTestHint(Class<?> clazz) {
        return Optional.ofNullable(clazz.getAnnotation(CamelKarafTestHint.class));
    }

    /**
     * @return the option to enable only the Camel route suppliers provided in the {@link CamelKarafTestHint} annotation.
     */
    private Option getCamelRouteSupplierFilter() {
        return CoreOptions.systemProperty(CAMEL_KARAF_INTEGRATION_TEST_ROUTE_SUPPLIERS_PROPERTY)
                .value(String.join(",", getCamelKarafTestHint().orElseThrow().camelRouteSuppliers()));
    }

    /**
     * Indicates whether specific Camel route suppliers have been provided in the {@link CamelKarafTestHint} annotation.
     * @return {@code true} if specific Camel route suppliers have been provided, {@code false} otherwise
     */
    private boolean hasCamelRouteSupplierFilter() {
        return getCamelKarafTestHint().filter(hint -> hint.camelRouteSuppliers().length > 0).isPresent();
    }

    /**
     * Indicates whether all Camel route suppliers should be ignored or not. By default, all Camel route suppliers are used.
     * @return {@code true} if all Camel route suppliers should be ignored, {@code false} otherwise
     */
    private boolean ignoreCamelRouteSuppliers() {
        return getCamelKarafTestHint().filter(CamelKarafTestHint::ignoreRouteSuppliers).isPresent();
    }

    /**
     * Indicates whether additional required features have been provided in the {@link CamelKarafTestHint} annotation.
     * @return {@code true} if additional required features have been provided, {@code false} otherwise
     */
    private boolean hasAdditionalRequiredFeatures() {
        return getCamelKarafTestHint().filter(hint -> hint.additionalRequiredFeatures().length > 0).isPresent();
    }

    /**
     * @return the option to ignore all Camel route suppliers.
     */
    private Option getIgnoreCamelRouteSupplier() {
        return CoreOptions.systemProperty(CAMEL_KARAF_INTEGRATION_TEST_IGNORE_ROUTE_SUPPLIERS_PROPERTY)
                .value(Boolean.toString(Boolean.TRUE));
    }

    /**
     * Returns the list of additional options to add to the configuration.
     */
    protected Option[] getAdditionalOptions() {
        return new Option[0];
    }

    @Before
    public final void init() throws Exception {
        installRequiredFeaturesRepositories();
        installRequiredFeatures();
        this.requiredBundles = installRequiredBundles();
    }

    /**
     * Gives the list of features repositories' URI to install that are required for the test.
     */
    protected List<String> getRequiredFeaturesRepositories() {
        return List.of();
    }

    /**
     * Install all the required features repositories.
     * @throws Exception if an error occurs while installing a features repository
     */
    private void installRequiredFeaturesRepositories() throws Exception {
        for (String featuresRepository : getRequiredFeaturesRepositories()) {
            addFeaturesRepository(featuresRepository);
        }
    }

    /**
     * Gives the list of features to install that are required for the test.
     */
    protected abstract List<String> getRequiredFeatures();

    /**
     * Gives the list of all required features including the additional features specified in
     * the {@link CamelKarafTestHint#additionalRequiredFeatures()}.
     */
    private List<String> getAllRequiredFeatures() {
        if (hasAdditionalRequiredFeatures()) {
            List<String> requiredFeatures = new ArrayList<>(getRequiredFeatures());
            requiredFeatures.addAll(List.of(getCamelKarafTestHint().orElseThrow().additionalRequiredFeatures()));
            return requiredFeatures;
        }
        return getRequiredFeatures();
    }

    /**
     * Installs the required features for the test.
     * @throws Exception if an error occurs while installing a feature
     */
    private void installRequiredFeatures() throws Exception {
        for (String featureName : getAllRequiredFeatures()) {
            if (featureService.getFeature(featureName) == null) {
                throw new IllegalArgumentException("Feature %s is not available".formatted(featureName));
            }
            installAndAssertFeature(featureName);
        }
    }

    /**
     * Installs the required bundles for the test.
     *
     * @return the list of the name of the installed bundles
     * @throws Exception if an error occurs while installing a bundle
     */
    protected List<String> installRequiredBundles() throws Exception {
        return List.of();
    }

    /**
     * Indicates whether the test is a blueprint test or not. By default, it's not a blueprint test.
     * @return {@code true} if the test is a blueprint test, {@code false} otherwise
     */
    private static boolean isBlueprintTest(Class<?> clazz) {
        return getCamelKarafTestHint(clazz).filter(CamelKarafTestHint::isBlueprintTest).isPresent();
    }

    private static Mode getMode(Class<?> clazz) {
        return getMode(isBlueprintTest(clazz));
    }

    private static Mode getMode(boolean blueprint) {
        return blueprint ? Mode.BLUEPRINT : Mode.CORE;
    }

    private Mode getMode() {
        return getMode(getClass());
    }

    @After
    public final void destroy()  {
        destroyProducerTemplates();
        uninstallRequiredBundles();
        uninstallRequiredFeatures();
        removeRequiredFeaturesRepositories();
    }

    private void uninstallRequiredBundles() {
        if (requiredBundles == null) {
            return;
        }
        for (String bundleName : requiredBundles) {
            try {
                uninstallBundle(bundleName);
            } catch (Exception e) {
                LOG.warn("Error while uninstalling bundle {}", bundleName, e);
            }
        }
    }

    private void uninstallBundle(String bundleName) {
        Bundle bundle = findBundleByName(bundleName);
        if (bundle == null) {
            return;
        }
        try {
            bundle.uninstall();
        } catch (BundleException e) {
            LOG.warn("Error while uninstalling bundle {}", bundleName, e);
        }
    }

    private void uninstallRequiredFeatures() {
        for (String featureName : getAllRequiredFeatures()) {
            try {
                featureService.uninstallFeature(featureName);
            } catch (Exception e) {
                LOG.warn("Error while uninstalling feature {}", featureName, e);
            }
        }
    }

    private void removeRequiredFeaturesRepositories() {
        for (String featuresRepository : getRequiredFeaturesRepositories()) {
            try {
                featureService.removeRepository(new URI(featuresRepository));
            } catch (Exception e) {
                LOG.warn("Error while removing features repository {}", featuresRepository, e);
            }
        }
    }

    private void destroyProducerTemplates() {
        templates.values().forEach(ProducerTemplate::stop);
        templates.clear();
    }

    protected void assertBundleInstalledAndRunning(String name) {
        Bundle bundle = findBundleByName(name);
        Assert.assertNotNull("Bundle %s should be installed".formatted(name), bundle);
        Assert.assertEquals(Bundle.ACTIVE, bundle.getState());
        //need to check with the command because the status may be Active while it's displayed as Waiting in the console
        //because of an exception for instance
        String bundles = executeCommand("bundle:list -s -t 0 | grep %s".formatted(name), timeoutInMillis(), false);
        Assert.assertTrue("bundle %s is in state %d /%s".formatted(bundle.getSymbolicName(), bundle.getState(), bundles),
                bundles.contains("Active"));
    }

    @Override
    public CamelContext getContext(Class<?> clazz) {
        return contexts.computeIfAbsent(new CamelContextKey(clazz), key -> {
            try {
                return getMode(clazz).getCamelContextClass(bundleContext, key.getCamelContextName());
            } catch (InvalidSyntaxException e) {
                throw new IllegalStateException("No CamelContext could be found matching the criteria", e);
            }
        });
    }

    @Override
    public ProducerTemplate getTemplate(Class<?> clazz) {
        return templates.computeIfAbsent(new CamelContextKey(clazz),
                key -> {
                    ProducerTemplate template = getContext(clazz).createProducerTemplate();
                    template.start();
                    return template;
                });
    }

    @Override
    public CamelContext getContext(String name, boolean isBlueprintTest) {
        return contexts.computeIfAbsent(new CamelContextKey(name, isBlueprintTest), key -> {
            try {
                return getMode(isBlueprintTest).getCamelContextClass(bundleContext, key.getCamelContextName());
            } catch (InvalidSyntaxException e) {
                throw new IllegalStateException("No CamelContext could be found matching the criteria", e);
            }
        });
    }

    @Override
    public ProducerTemplate getTemplate(String name, boolean isBlueprintTest) {
        return templates.computeIfAbsent(new CamelContextKey(name, isBlueprintTest),
                key -> {
                    ProducerTemplate template = getContext(name, isBlueprintTest).createProducerTemplate();
                    template.start();
                    return template;
                });
    }

    private enum Mode {
        BLUEPRINT {
            @Override
            String getFeatureName() {
                return "camel-blueprint";
            }

            @Override
            Class<? extends CamelContext> getCamelContextClass() {
                return BlueprintCamelContext.class;
            }
        },
        CORE {
            @Override
            String getFeatureName() {
                return "camel-core";
            }

            @Override
            Class<? extends CamelContext> getCamelContextClass() {
                return OsgiDefaultCamelContext.class;
            }
        };

        abstract String getFeatureName();

        abstract Class<? extends CamelContext> getCamelContextClass();

        CamelContext getCamelContextClass(BundleContext bundleContext, String name) throws InvalidSyntaxException {
            for (int i = 0; i < getContextFinderRetry(); i++) {
                CamelContext camelContext = findCamelContext(bundleContext, name);
                if (camelContext != null) {
                    return camelContext;
                }
                LOG.warn("No CamelContext could be found matching the criteria (mode = {}, name = {}), retrying in {} seconds",
                        this, name, getContextFinderRetryInterval());
                try {
                    Thread.sleep(getContextFinderRetryInterval() * 1_000L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }

            throw new IllegalStateException("No CamelContext could be found matching the criteria (mode = " + this + ", name = " + name + ")");
        }

        private CamelContext findCamelContext(BundleContext bundleContext, String name) throws InvalidSyntaxException {
            ServiceReference<?>[] references = bundleContext.getServiceReferences(CamelContext.class.getName(), null);
            if (references == null) {
                return null;
            }
            for (ServiceReference<?> reference : references) {
                if (reference == null) {
                    continue;
                }
                CamelContext camelContext = (CamelContext) bundleContext.getService(reference);
                if (camelContext.getClass().equals(getCamelContextClass())
                        && (name == null || name.equals(camelContext.getName()))) {
                    return camelContext;
                }
            }
            return null;
        }
    }

    private static class CamelContextKey {
        private final String name;
        private final boolean blueprint;

        CamelContextKey(String name, boolean blueprint) {
            this.name = name;
            this.blueprint = blueprint;
        }

        CamelContextKey(Class<?> clazz) {
            this(Utils.getCamelContextName(clazz), isBlueprintTest(clazz));
        }

        public String getCamelContextName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CamelContextKey that = (CamelContextKey) o;
            return blueprint == that.blueprint && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, blueprint);
        }
    }
}