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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.osgi.framework.Bundle;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.ops4j.pax.exam.Configuration;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.osgi.framework.BundleException;

import static org.apache.karaf.camel.itests.Utils.toKebabCase;
import static org.ops4j.pax.exam.OptionUtils.combine;

public abstract class AbstractCamelKarafITest extends KarafTestSupport {

    protected CamelContext context;
    protected ProducerTemplate template;

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

    public String getTestComponentName() {
        return toKebabCase(this.getClass().getSimpleName()).replace("-itest", "-test");
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
        // Workaround to fix the issue with the port already in use
        String httpPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_HTTP_PORT), Integer.parseInt(MAX_HTTP_PORT)));
        String rmiRegistryPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_RMI_REG_PORT), Integer.parseInt(MAX_RMI_REG_PORT)));
        String rmiServerPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_RMI_SERVER_PORT), Integer.parseInt(MAX_RMI_SERVER_PORT)));
        String sshPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_SSH_PORT), Integer.parseInt(MAX_SSH_PORT)));

        Option[] options = new Option[]{
                CoreOptions.systemProperty("project.version").value(getVersion()),
                CoreOptions.systemProperty("project.target").value(getBaseDir()),
                KarafDistributionOption.features("mvn:org.apache.camel.karaf/apache-camel/"+ getVersion() + "/xml/features", "scr","camel-core"),
                CoreOptions.mavenBundle().groupId("org.apache.camel.karaf").artifactId("camel-integration-test").versionAsInProject(),
                KarafDistributionOption.editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", httpPort),
                KarafDistributionOption.editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
                KarafDistributionOption.editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
                KarafDistributionOption.editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort)
        };
        Option[] systemProperties = PaxExamWithExternalResource.systemProperties().entrySet().stream()
                .map(e -> CoreOptions.systemProperty(e.getKey()).value(e.getValue()))
                .toArray(Option[]::new);
        return combine(combine(super.config(), options), systemProperties);
    }

    @Before
    public void init() throws Exception {
        String testComponentName = getTestComponentName();
        installRequiredFeatures();
        installBundle("file://%s/%s-%s.jar".formatted(getBaseDir(), testComponentName, getVersion()), true);
        assertBundleInstalledAndRunning(testComponentName);
        initCamelContext();
        initProducerTemplate();
    }

    protected void installRequiredFeatures() throws Exception {
        String featureName = toKebabCase(this.getClass().getSimpleName()).replace("-itest", "");
        if (null != featureService.getFeature(featureName)) {
            installAndAssertFeature(featureName);
        }
    }

    private void initCamelContext() {
        this.context = ServiceLookup.getService(bundleContext, CamelContext.class);
    }

    private void initProducerTemplate() {
        template = context.createProducerTemplate();
        template.start();
    }

    @After
    public void destroy()  {
        destroyProducerTemplate();
        uninstallBundle();
    }

    private void destroyProducerTemplate() {
        if (template != null) {
            template.stop();
        }
    }

    private void uninstallBundle() {
        Bundle bundle = findBundleByName(getTestComponentName());
        if (bundle == null) {
            return;
        }
        try {
            bundle.uninstall();
        } catch (BundleException e) {
            // Ignore me
        }
    }

    public static int getAvailablePort(int min, int max) {
        for (int port = min; port <= max; port++) {
            try (ServerSocket socket = new ServerSocket()) {
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
                return socket.getLocalPort();
            } catch (Exception e) {
                System.err.println("Port " + port + " not available, trying next one");
            }
        }
        throw new IllegalStateException("Can't find available network ports");
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
}