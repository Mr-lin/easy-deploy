package com.github.mrlin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author linzhiwei
 * @Description:
 * @date 2019/2/26 9:39
 */
@Mojo(name = "java-deploy")
public class DeployPlugin extends AbstractMojo {

    @Parameter
    private String host;
    @Parameter
    private String user;
    @Parameter
    private String password;
    @Parameter
    private String targetName;
    @Parameter
    private String targetDir;
    @Parameter
    private String remoteDeployDir;
    @Parameter
    private String deployScript;
    @Parameter
    private String logPath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        //netstat -tunlp |grep 41020 |grep -v grep|awk '{print $7}'|awk -F '/java' '{print $1}'

        Deploy deploy = new Deploy(host,user,password);
        System.out.println(String.format("host:%s user:%s password:%s targetDir:%s",host,user,password,targetDir));
//        String osname = System.getProperty("os.name").toLowerCase();
//        String mvn = osname.contains("windows")?"mvn.cmd":"mvn";
//        int re = deploy.sysExecute(mvn+" -f E:\\projects\\esnotary-github-nzsign-v1.0\\pom.xml clean package -Pdev -Dmaven.github.skip=true");
//        if (re!=0){
//            System.err.println("package error");
//            System.exit(-1);
//        }
        deploy.uploadFile(targetDir,"/tmp");
        StringBuffer cmd = new StringBuffer();
        cmd.append(String.format("cd %s;",remoteDeployDir));
        cmd.append(String.format("echo %s|sudo -S mv /tmp/%s ./;",password,targetName));
        cmd.append(String.format("echo %s|sudo -S ./%s %s",password,deployScript,targetName));
        System.out.println("cmd:"+cmd.toString());
        deploy.shell(cmd.toString());
        if (!"".equals(logPath)){
            deploy.shell("tail -f "+logPath);
        }
        deploy.disconnect();
    }
}
