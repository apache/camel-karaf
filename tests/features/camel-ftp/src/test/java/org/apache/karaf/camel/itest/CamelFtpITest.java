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
package org.apache.karaf.camel.itest;

import java.util.Collections;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteITest;
import org.apache.karaf.camel.itests.CamelKarafTestHint;
import org.apache.karaf.camel.itests.PaxExamWithExternalResource;
import org.apache.karaf.camel.itests.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@CamelKarafTestHint(externalResourceProvider = CamelFtpITest.ExternalResourceProviders.class)
@RunWith(PaxExamWithExternalResource.class)
@ExamReactorStrategy(PerClass.class)
public class CamelFtpITest extends AbstractCamelSingleFeatureResultMockBasedRouteITest {


    @Override
    public void configureMock(MockEndpoint mock) {
        mock.expectedBodiesReceived("OK");
    }

    @Test
    public void testResultMock() throws Exception {
        assertMockEndpointsSatisfied();
    }

    public static final class ExternalResourceProviders {

        public static FtpServerResource createFtpServer() throws FtpException {

            final int ftpPort = Utils.getNextAvailablePort();
            NativeFileSystemFactory fsf = new NativeFileSystemFactory();
            fsf.setCreateHome(true);

            PropertiesUserManagerFactory pumf = new PropertiesUserManagerFactory();
            pumf.setAdminName("admin");
            pumf.setPasswordEncryptor(new ClearTextPasswordEncryptor());
            pumf.setFile(null);

            UserManager userMgr = pumf.createUserManager();

            BaseUser user = new BaseUser();
            user.setName("scott");
            user.setPassword("tiger");
            user.setHomeDirectory(System.getProperty("project.target"));
            user.setAuthorities(Collections.singletonList(new WritePermission()));
            userMgr.save(user);

            FtpServerFactory serverFactory = new FtpServerFactory();
            serverFactory.setUserManager(userMgr);
            serverFactory.setFileSystem(fsf);
            serverFactory.setConnectionConfig(new ConnectionConfigFactory().createConnectionConfig());

            ListenerFactory factory = new ListenerFactory();
            factory.setPort(ftpPort);
            factory.setServerAddress("127.0.0.1");

            final Listener listener = factory.createListener();

            serverFactory.addListener("default", listener);

            return new FtpServerResource(serverFactory.createServer(), ftpPort);

        }
    }
}