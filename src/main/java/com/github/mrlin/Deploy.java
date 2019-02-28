package com.github.mrlin;

import com.jcraft.jsch.*;

import java.io.*;
import java.util.Properties;

/**
 * @author linzhiwei
 * @Description:
 * @date 2019/2/26 11:28
 */
public class Deploy {

    private String host;
    private String user;
    private String password;
    private Session session;

    public Deploy(String host, String user, String password) {
        this.host = host;
        this.user = user;
        this.password = password;
        init();
    }

    /**
     * 上传文件到远程服务器
     *
     * @param src
     * @param dst
     * @return
     */
    public boolean uploadFile(String src, String dst) {
        ChannelSftp channelSftp = null;
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.setFilenameEncoding("UTF-8");

            channelSftp.put(src, dst, new SftpProgressMonitor() {

                long count = 0;
                long max = 0;
                long percent = -1;

                public void init(int op, String src, String dest, long max) {
                    this.max = max;
                }

                public boolean count(long count) {
                    this.count += count;
                    percent = this.count * 100 / max;
                    System.out.println("Completed " + this.count + "(" + percent + "%) out of " + max + ".");
                    //返回false 会终止传输
                    return true;
                }

                public void end() {
                    System.out.println("over");
                }
            });
            return true;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return false;
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.quit();
                channelSftp.disconnect();
            }
        }
    }

    /**
     * 超级权限执行命令
     *
     * @param command
     * @return
     */
    public boolean sudoExecute(String command) {
        return execute(command, true);
    }

    public boolean execute(String command) {
        return execute(command, false);
    }

    /**
     * 普通命令执行
     *
     * @param command
     * @return
     */
    private boolean execute(String command, boolean sudo) {
        Channel channel = null;
        try {
            channel = session.openChannel("exec");
            if (sudo) {
                command = "sudo -S -p '' " + command;
            }
            ((ChannelExec) channel).setCommand(command);

            InputStream in = channel.getInputStream();
            OutputStream out = channel.getOutputStream();
            ((ChannelExec) channel).setErrStream(System.err);
            channel.connect();

            if (sudo) {
                out.write((password + "\n").getBytes());
                out.flush();
            }

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    System.out.print(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    System.out.println("exit-status: " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            return false;
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
        return true;
    }

    /**
     * 执行远程shell
     *
     * @param command
     * @return
     */
    public boolean shell(String command) {
        ChannelShell channel = null;
        try {
            channel = (ChannelShell) session.openChannel("shell");
            channel.setPty(true);
            channel.setOutputStream(System.out, true);
            channel.connect();
            InputStream in = channel.getInputStream();
            OutputStream os = channel.getOutputStream();

            PrintWriter printWriter = new PrintWriter(os, true);
            byte[] tmp = new byte[1024];
            String ss[] = command.split(";");
            for (String s : ss) {
                printWriter.println(s);
            }
            printWriter.println("exit");
            printWriter.close();
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    System.out.println(new String(tmp));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            return false;
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
        return true;
    }

    /**
     * 系统命令执行
     *
     * @return
     */
    public int sysExecute(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            final InputStream is = process.getInputStream();
            final InputStream erris = process.getErrorStream();
            //执行输出
            new Thread(() -> {
                BufferedReader br1 = new BufferedReader(new InputStreamReader(is));
                try {
                    String line = null;
                    while ((line = br1.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            //错误输出
            new Thread(() -> {
                BufferedReader br1 = new BufferedReader(new InputStreamReader(erris));
                try {
                    String line = null;
                    while ((line = br1.readLine()) != null) {
                        System.err.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        erris.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            process.waitFor();

            return process.exitValue();

        } catch (Exception e) {
            System.err.println(String.format("执行命令行%s出错，详情：%s", command, e.getMessage()));
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return -1;
    }

    /**
     * 连接初始化
     */
    private void init() {
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host);
            session.setPassword(password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
        } catch (Exception e) {
            throw new RuntimeException("连接异常");
        }
    }

    /**
     * 关闭连接
     */
    public void disconnect() {
        session.disconnect();
    }
}
