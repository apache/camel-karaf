package org.apache.karaf.camel.itest;

import java.util.Map;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.karaf.camel.itests.ExternalResource;

public class FtpServerResource implements ExternalResource {

    private final FtpServer ftpServer;
    private final int port;

    public FtpServerResource(FtpServer server, int port) {
        this.ftpServer = server;
        this.port = port;
    }

    @Override
    public void before() {
        try {
            ftpServer.start();
        } catch (FtpException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void after() {
        ftpServer.stop();
    }

    @Override
    public Map<String, String> properties() {
        return Map.of("ftp.port", Integer.toString(port));
    }
}
