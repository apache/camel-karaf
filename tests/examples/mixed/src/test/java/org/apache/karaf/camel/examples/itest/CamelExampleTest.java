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
package org.apache.karaf.camel.examples.itest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.apache.karaf.camel.itests.AbstractCamelRouteWithBundleITest;
import org.apache.karaf.camel.itests.AvailablePortProvider;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.assertEquals;


@CamelKarafTestHint(isBlueprintTest = true, externalResourceProvider = CamelExampleTest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelExampleTest extends AbstractCamelRouteWithBundleITest {

    public static final String BLUEPRINT_DSL_JETTY_PORT = "blueprint.dsl.jetty.port";
    public static final String JAVA_DSL_JETTY_PORT = "java.dsl.jetty.port";

    @Override
    protected List<String> getRequiredFeatures() {
        return List.of("camel-jetty", "camel-jsonpath", "camel-mail");
    }

    @Override
    protected String getTestBundleName() {
        return "camel-karaf-examples-mixed-test";
    }

    @Test(timeout = 60000)
    public void testBlueprintDSL() throws Exception {
        verify(System.getProperty(BLUEPRINT_DSL_JETTY_PORT));
    }

    @Test(timeout = 60000)
    public void testJavaDSL() throws Exception {
        verify(System.getProperty(JAVA_DSL_JETTY_PORT));
    }

    private static void verify(String port) throws IOException {
        URL url = new URL("http://localhost:%s/example".formatted(port));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setDoInput(true);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream()))) {
            writer.println("{ \"notification\": { \"type\": \"email\", \"to\": \"foo@bar.com\", \"message\": \"this is a test\" }}");
            writer.flush();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            StringBuilder buffer = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            assertEquals("{ \"status\": \"email sent\", \"to\": \"foo@bar.com\", \"subject\": \"Notification\" }", buffer.toString());
        }

        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setDoInput(true);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream()))) {
            writer.println("{ \"notification\": { \"type\": \"http\", \"service\": \"http://foo\" }}");
            writer.flush();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            assertEquals("{ \"status\": \"http requested\", \"service\": \"http://foo\" }", buffer.toString());
        }
    }

    public static final class ExternalResourceProviders {

        public static AvailablePortProvider createAvailablePortProvider() {
            return new AvailablePortProvider(List.of(BLUEPRINT_DSL_JETTY_PORT, JAVA_DSL_JETTY_PORT));
        }
    }
}