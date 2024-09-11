package com.hew.hewojcodesandbox.security;

import java.security.Permission;

/**
 * 对提交的代码，自定义哪些权限可以放开，哪些权限要禁止
 */
public class MySecurityManager extends SecurityManager {


    // 检查所有的权限
    @Override
    public void checkPermission(Permission perm) {
//        super.checkPermission(perm);
    }

    // 检测程序是否可执行文件。检测到是一个可执行文件如果要拒绝就直接抛异常
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("checkExec 权限异常：" + cmd);
    }

    // 检测程序是否允许读文件

    @Override
    public void checkRead(String file) {
        System.out.println(file);
        if (file.contains("/Users/hewei/IdeaWorkSpace/hewoj-code-sandbox")) {
            return;
        }
        throw new SecurityException("checkRead 权限异常：" + file);
    }

    // 检测程序是否允许写文件，直接抛异常表示不允许程序写操作，不抛异常表示允许程序写操作
    @Override
    public void checkWrite(String file) {
//        throw new SecurityException("checkWrite 权限异常：" + file);
    }

    // 检测程序是否允许删除文件
    @Override
    public void checkDelete(String file) {

    }

    // 检测程序是否允许连接网络
    @Override
    public void checkConnect(String host, int port) {
//        throw new SecurityException("checkConnect 权限异常：" + host + ":" + port);
    }
}
