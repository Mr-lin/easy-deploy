package com.github.mrlin.easydeploy;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * @author linzhiwei
 * @version 1.0
 */
@Mojo(name = "java-deploy")
public class DeployPlugin extends AbstractMojo {

    @Parameter
    private String host;
    @Parameter
    private String user;
    @Parameter
    private String password;
    @Parameter(defaultValue = "${project.build.finalName}.${pom.packaging}")
    private String targetName;
    @Parameter(defaultValue = "${project.build.directory}")
    private String targetDir;
    @Parameter
    private String remoteDeployDir;
    @Parameter
    private String deployScriptParameter;
    @Parameter
    private String deployScript;
    @Parameter
    private String logPath;
    @Parameter
    private boolean isRoot;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        //netstat -tunlp |grep 41020 |grep -v grep|awk '{print $7}'|awk -F '/java' '{print $1}'

        Deploy deploy = new Deploy(host,user,password);
        System.out.println(String.format("host:%s user:%s password:%s targetDir:%s targetName:%s",host,user,password,targetDir,targetName));
//        String osname = System.getProperty("os.name").toLowerCase();
//        String mvn = osname.contains("windows")?"mvn.cmd":"mvn";
//        int re = deploy.sysExecute(mvn+" -f E:\\projects\\esnotary-github-nzsign-v1.0\\pom.xml clean package -Pdev -Dmaven.github.skip=true");
//        if (re!=0){
//            System.err.println("package error");
//            System.exit(-1);
//        }
        deploy.uploadFile(targetDir+ File.separator + targetName,"/tmp");
        StringBuffer cmd = new StringBuffer();
        cmd.append(String.format("cd %s;",remoteDeployDir));
        if (isRoot){
            cmd.append(String.format("mv -f /tmp/%s ./;",targetName));
            cmd.append(String.format("%s",deployScript));
        }else {
            cmd.append(String.format("echo %s|sudo -S mv -f /tmp/%s ./;",password,targetName));
            cmd.append(String.format("echo %s|sudo -S %s",password,deployScript));
        }
        if (deployScriptParameter!=null&&!"".equals(deployScriptParameter)){
            cmd.append(" "+deployScriptParameter);
        }
        System.out.println("cmd:"+cmd.toString());
        deploy.shell(cmd.toString());
        if (logPath!=null&&!"".equals(logPath)){
            deploy.shell("tail -f "+logPath);
        }
        deploy.disconnect();
    }
}
