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
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.blueprint.BlueprintCamelContext;
import org.apache.camel.karaf.core.OsgiDefaultCamelContext;
import org.apache.karaf.itests.KarafTestSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFilePutOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import static org.ops4j.pax.exam.OptionUtils.combine;

public abstract class AbstractCamelRouteITest extends KarafTestSupport {

    public static final int CAMEL_KARAF_INTEGRATION_TEST_DEBUG_DEFAULT_PORT = 8889;
    public static final String CAMEL_KARAF_INTEGRATION_TEST_DEBUG_PROPERTY = "camel.karaf.itest.debug";

    protected CamelContext context;
    protected ProducerTemplate template;
    private List<String> requiredBundles;

    public String getVersion() {
        return System.getProperty("project.version");
    }

    public String getBaseDir() {
        return System.getProperty("project.target");
    }

    public File getUsersFile() {
        // retrieve the users.properties file from the resources folder to avoid file duplication
        return new File(System.getProperty("integration.test.project.resources"), "etc/users.properties");
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
        Option[] options = new Option[]{
            CoreOptions.systemProperty("project.version").value(getVersion()),
            CoreOptions.systemProperty("project.target").value(getBaseDir()),
            KarafDistributionOption.features("mvn:org.apache.camel.karaf/apache-camel/%s/xml/features".formatted(getVersion()), "scr", getMode().getFeatureName()),
            CoreOptions.mavenBundle().groupId("org.apache.camel.karaf").artifactId("camel-integration-test").versionAsInProject(),
        };
        Option[] combine = combine(updatePorts(super.config()), options);
        if (isDebugModeEnabled()) {
            combine = combine(combine, KarafDistributionOption.debugConfiguration(Integer.toString(getDebugPort()), true));
        }
        if (hasExternalResources()) {
            return combine(combine, getExternalResourceOptions());
        }
        return combine;
    }

    /**
     * Indicates whether the debug mode is enabled or not. The debug mode is enabled when the system property
     * {@link #CAMEL_KARAF_INTEGRATION_TEST_DEBUG_PROPERTY} is set.
     * @return {@code true} if the debug mode is enabled, {@code false} otherwise
     */
    private boolean isDebugModeEnabled() {
        return System.getProperty(CAMEL_KARAF_INTEGRATION_TEST_DEBUG_PROPERTY) != null;
    }

    /**
     * Returns the debug port to use when the debug mode is enabled, corresponding to the value of the system property
     * {@link #CAMEL_KARAF_INTEGRATION_TEST_DEBUG_PROPERTY}. The default value is {@link #CAMEL_KARAF_INTEGRATION_TEST_DEBUG_DEFAULT_PORT}.
     * @return the debug port
     */
    private int getDebugPort() {
        return Integer.getInteger(CAMEL_KARAF_INTEGRATION_TEST_DEBUG_PROPERTY, CAMEL_KARAF_INTEGRATION_TEST_DEBUG_DEFAULT_PORT);
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

    @NotNull
    private static Option[] getExternalResourceOptions() {
        return PaxExamWithExternalResource.systemProperties().entrySet().stream()
                .map(e -> CoreOptions.systemProperty(e.getKey()).value(e.getValue()))
                .toArray(Option[]::new);
    }

    private boolean hasExternalResources() {
        CamelKarafTestHint hint = getClass().getAnnotation(CamelKarafTestHint.class);
        return hint != null && hint.externalResourceProvider() != Object.class;
    }

    @Before
    public final void init() throws Exception {
        installRequiredFeaturesRepositories();
        installRequiredFeatures();
        this.requiredBundles = installRequiredBundles();
        initCamelContext();
        initProducerTemplate();
    }

    /**
     * Gives the list of features repositories' URI to install that are required for the test.
     */
    protected List<String> getRequiredFeaturesRepositories() {
        return List.of();
    }

    private void installRequiredFeaturesRepositories() throws Exception {
        for (String featuresRepository : getRequiredFeaturesRepositories()) {
            addFeaturesRepository(featuresRepository);
        }
    }

    /**
     * Gives the list of features to install that are required for the test.
     */
    protected abstract List<String> getRequiredFeatures();

    private void installRequiredFeatures() throws Exception {
        for (String featureName : getRequiredFeatures()) {
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
    private boolean isBlueprintTest() {
        CamelKarafTestHint hint = getClass().getAnnotation(CamelKarafTestHint.class);
        return hint != null && hint.isBlueprintTest();
    }

    private Mode getMode() {
        return isBlueprintTest() ? Mode.BLUEPRINT : Mode.CORE;
    }

    private void initCamelContext() throws InvalidSyntaxException {
        this.context = getMode().getCamelContextClass(bundleContext);
    }

    private void initProducerTemplate() {
        template = context.createProducerTemplate();
        template.start();
    }

    @After
    public final void destroy()  {
        destroyProducerTemplate();
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
                // Ignore me
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
            // Ignore me
        }
    }

    private void uninstallRequiredFeatures() {
        for (String featureName : getRequiredFeatures()) {
            try {
                featureService.uninstallFeature(featureName);
            } catch (Exception e) {
                // Ignore me
            }
        }
    }

    private void removeRequiredFeaturesRepositories() {
        for (String featuresRepository : getRequiredFeaturesRepositories()) {
            try {
                featureService.removeRepository(new URI(featuresRepository));
            } catch (Exception e) {
                // Ignore me
            }
        }
    }

    private void destroyProducerTemplate() {
        if (template != null) {
            template.stop();
        }
    }

    protected void assertBundleInstalledAndRunning(String name) {
        Bundle bundle = findBundleByName(name);
        Assert.assertNotNull("Bundle %s should be installed".formatted(name), bundle);
        Assert.assertEquals(Bundle.ACTIVE, bundle.getState());
        //need to check with the command because the status may be Active while it's displayed as Waiting in the console
        //because of an exception for instance
        String bundles = executeCommand("bundle:list -s -t 0 | grep %s".formatted(name));
        Assert.assertTrue("bundle %s is in state %d /%s".formatted(bundle.getSymbolicName(), bundle.getState(), bundles),
                bundles.contains("Active"));
    }

    public CamelContext getContext() {
        return context;
    }

    public ProducerTemplate getTemplate() {
        return template;
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

        CamelContext getCamelContextClass(BundleContext bundleContext) throws InvalidSyntaxException {
            ServiceReference<?>[] references = bundleContext.getServiceReferences(CamelContext.class.getName(), null);
            if (references == null) {
                throw new IllegalStateException("No CamelContext available");
            }
            for (ServiceReference<?> reference : references) {
                if (reference == null) {
                    continue;
                }
                CamelContext camelContext = (CamelContext) bundleContext.getService(reference);
                if (camelContext.getClass().equals(getCamelContextClass())) {
                    return camelContext;
                }
            }
            throw new IllegalStateException("No CamelContext available");
        }
    }
}