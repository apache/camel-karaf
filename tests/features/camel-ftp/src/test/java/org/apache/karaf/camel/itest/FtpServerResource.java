package org.apache.karaf.camel.itest;

import static org.apache.karaf.camel.itest.CamelFtpITest.FTP_PORT_PROPERTY;

import java.util.Collections;
import java.util.List;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
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
import org.apache.karaf.camel.itests.AvailablePortProvider;
import org.apache.karaf.camel.itests.ExternalResource;
import org.apache.karaf.camel.itests.ExternalResourceWithPrerequisite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FtpServerResource extends ExternalResourceWithPrerequisite {

    private static final Logger LOG = LoggerFactory.getLogger(FtpServerResource.class);
    private static final AvailablePortProvider portProvider = new AvailablePortProvider(List.of(FTP_PORT_PROPERTY));
    private FtpServer ftpServer;

    @Override
    protected void doStart() {
        try {
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
            factory.setPort(Integer.parseInt(properties().get(FTP_PORT_PROPERTY)));
            factory.setServerAddress("127.0.0.1");

            final Listener listener = factory.createListener();
            serverFactory.addListener("default", listener);

            ftpServer = serverFactory.createServer();
            ftpServer.start();
        } catch (FtpException e) {
            LOG.error("Could not create FTP server",e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doStop() {
        if (ftpServer != null) {
            ftpServer.stop();
        }
    }

    @Override
    protected List<ExternalResource> getPrerequisites() {
        return List.of(portProvider);
    }

}
