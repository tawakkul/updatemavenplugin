package com.update.maven;

import com.jcraft.jsch.*;

import java.util.Properties;
import java.util.function.Consumer;

public class Sftp {
    private String username;
    private String password;
    private String host;
    private int port;

    public Sftp(String username, String password, String host, int port) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
    }

    public void run(Consumer<ChannelSftp> consumer ) throws JSchException {
            JSch jsch = new JSch();
            Session session = jsch.getSession( this.username, this.host, this.port);
            session.setPassword(this.password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            consumer.accept((ChannelSftp)channel);
            channel.disconnect();
            session.disconnect();
    }
}
