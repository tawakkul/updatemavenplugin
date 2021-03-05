package com.update.maven;

import com.jcraft.jsch.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.StringUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

/**
 * @author huayu
 */
@Mojo(name="update",defaultPhase = LifecyclePhase.PACKAGE)
public class Update extends AbstractMojo {

    @Parameter(required = true)
    String targetDir;
    @Parameter(required = true)
    String sourceFile;
    @Parameter(defaultValue = "root")
    String username;
    @Parameter(required = true)
    String password;
    @Parameter(required = true)
    String host ;
    @Parameter(defaultValue = "22")
    int port;
    String fileName;
    String dirName;

    public static void main(String[] args) throws MojoFailureException, MojoExecutionException {
        new Update().execute();
    }


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            init();
            new Shell(username, password, host, port).run(this::shellStop);
            new Sftp(username, password, host, port).run(this::sftp);
            new Shell(username, password, host, port).run(this::shellStart);
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        String[] sourcePath = sourceFile.split("[\\\\/]",-1);
        fileName =sourcePath[sourcePath.length-1];
        if(fileName.endsWith(".tar.gz")){
            dirName = StringUtils.stripEnd(fileName,".tar.gz");
        }

    }

    private void shellStop(ChannelShell shell) {
        try(PrintWriter pw = new PrintWriter(shell.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(shell.getInputStream())) ){
            pw.println("cd "+targetDir);
            pw.println("cd "+dirName);
            pw.println("cd bin");
            pw.println("sh stop.sh");
            pw.println("exit ");
            pw.flush();
            br.lines().forEach(System.out::println);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void shellStart(ChannelShell shell) {
        try(PrintWriter pw = new PrintWriter(shell.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(shell.getInputStream())) ){
           pw.println("cd "+targetDir);
           pw.println("tar -zxvf "+fileName);
           pw.println("cd "+dirName);
           pw.println("cd bin");
           pw.println("sh start.sh");
           pw.println("exit ");
           pw.flush();
           br.lines().forEach(System.out::println);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private void sftp(ChannelSftp channelSftp) {
        try {
            channelSftp.cd("/");
            String[] paths = Arrays.stream(targetDir.split("[\\\\/]", -1)).filter(org.apache.commons.lang3.StringUtils::isNotEmpty).toArray(String[]::new);

            for(int i=0; i<paths.length ;i++){
                    Vector vector = channelSftp.ls(channelSftp.pwd());
                    int finalI = i;
                    if(!vector.stream().anyMatch(value->  StringUtils.equals(((ChannelSftp.LsEntry) value).getFilename(),paths[finalI]))){
                        channelSftp.mkdir(paths[finalI]);
                    }
                    channelSftp.cd(paths[finalI]);
            }

            Date date = new Date();
            Vector vector = channelSftp.ls(channelSftp.pwd());
            if(vector.stream().anyMatch(value->  StringUtils.equals(((ChannelSftp.LsEntry) value).getFilename(),fileName))){
                channelSftp.rename(fileName,fileName+".bak."+new SimpleDateFormat("yyyyMMddHHmmss").format(date));
            }

            String finalDirName = dirName;
            if(vector.stream().anyMatch(value->  StringUtils.equals(((ChannelSftp.LsEntry) value).getFilename(), finalDirName))){
                channelSftp.rename(finalDirName,finalDirName+".bak."+new SimpleDateFormat("yyyyMMddHHmmss").format(date));
            }


            try(FileInputStream fileInputStream = new FileInputStream(sourceFile)){
                channelSftp.put(fileInputStream,fileName);
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }
    }
}
