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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.osgi.framework.Bundle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.ops4j.pax.exam.Configuration;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;

public class CamelKarafITest extends KarafTestSupport {

    public String getVersion() {
        return System.getProperty("project.version");
    }

    public String getBaseDir() {
        return System.getProperty("project.target");
    }


    @Override
    public File getConfigFile(String path) {
        //to be able to have a single users.properties files for all tests
        if (path.endsWith("users.properties")) {
            try (InputStream etcStream = CamelKarafITest.class.getResourceAsStream(path)) {
                if (etcStream != null) {
                    Path tmpFile = Files.createTempFile("etc", "properties");
                    Files.copy(etcStream, tmpFile, StandardCopyOption.REPLACE_EXISTING);
                    return tmpFile.toFile();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        URL res = this.getClass().getResource(path);
        if (res == null) {
            throw new RuntimeException("Config resource " + path + " not found");
        }
        return new File(res.getFile());
    }

    @Configuration
    @Override
    public Option[] config() {
        Option[] options = new Option[]{
                KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "project.version", getVersion()),
                KarafDistributionOption.editConfigurationFileExtend("etc/system.properties", "project.target", getBaseDir()),
                KarafDistributionOption.features("mvn:org.apache.camel.karaf/apache-camel/"+ getVersion() + "/xml/features", "scr","camel-core"),
                CoreOptions.mavenBundle().groupId("org.apache.camel.karaf").artifactId("camel-integration-test").versionAsInProject(),
        };
        return Stream.of(super.config(), options).flatMap(Stream::of).toArray(Option[]::new);
    }

    public void assertBundleInstalledAndRunning(String name) {
        Bundle bundle = findBundleByName(name);
        Assert.assertNotNull("Bundle " + name + " should be installed", bundle);
        Assert.assertEquals(Bundle.ACTIVE, bundle.getState());
        //need to check with the command because the status may be Active while it's displayed as Waiting in the console
        //because of an exception for instance
        String bundles = executeCommand("bundle:list -s -t 0 |grep "+name);
        Assert.assertTrue("bundle"+ bundle.getSymbolicName()+ " is in state " + bundle.getState() + " /" + bundles,
                bundles.contains("Active"));
    }

}