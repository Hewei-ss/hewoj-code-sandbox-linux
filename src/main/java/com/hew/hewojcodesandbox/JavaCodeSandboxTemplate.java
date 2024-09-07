package com.hew.hewojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.hew.hewojcodesandbox.model.ExecuteCodeRequest;
import com.hew.hewojcodesandbox.model.ExecuteCodeResponse;
import com.hew.hewojcodesandbox.model.ExecuteMessage;
import com.hew.hewojcodesandbox.model.JudgeInfo;
import com.hew.hewojcodesandbox.model.enums.ExecuteCodeStatusEnum;
import com.hew.hewojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.hew.hewojcodesandbox.constant.SandBoxConstants.TIME_OUT;


@Slf4j
public abstract class JavaCodeSandboxTemplate implements Codesandbox{

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

//    private static final long TIME_OUT = 5000L;
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) throws IOException {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        // 1.将用户提交的代码文件保存为文件
        File userCodeFile=saveCodeToFile(code);
        //2.编译代码得到class文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        //如果编译失败直接返回
        if(compileFileExecuteMessage.getExitValue() != 0){
            JudgeInfo judgeInfo=new JudgeInfo();
            judgeInfo.setMessage(compileFileExecuteMessage.getMessage());
            return ExecuteCodeResponse.builder()
                    .status(ExecuteCodeStatusEnum.COMPILE_FAILED.getValue())
                    .judgeInfo(judgeInfo)
                    .build();
        }
        System.out.println(compileFileExecuteMessage);
        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);
        //        4. 收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);
        //  5. 文件清理
        boolean b = deleteFile(userCodeFile);
        if (!b) {
            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }
        return outputResponse;
    }
    /**
     * 1. 将用户提交的代码文件保存为文件
     * @param code
     * @return
     */
    public File saveCodeToFile(String code){
        // 1.将用户提交的代码文件保存为文件
        String userDir = System.getProperty("user.dir");

        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        //把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     *   //2.编译代码得到class文件
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile){
        // 定义编译的指令
        String complicCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        Process compliProcess = null;
        try {
            //执行指令
            //使用当前运行时对象来执行一个系统命令，该命令由 compileCmd 字符串指定。返回一个 Process 对象，
            // 代表这个正在执行的进程。通过这个对象，你可以获取进程的输入输出流、等待进程完成或者销毁进程等。
            compliProcess = Runtime.getRuntime().exec(complicCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compliProcess, "编译");
            System.out.println(executeMessage);
            return executeMessage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * // 3. 执行代码，得到输出结果
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage>runFile(File userCodeFile,List<String> inputList) throws IOException {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        //3. 执行代码返回结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String input : inputList) {
            //String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, input);
            //在要运行移交代码时，在指令中指定开启安全管理器
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath);
          //  String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main", dir, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME);
           // String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main", dir, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                //防止出现死循环，在限定时间销毁进程
                 new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超出沙箱最大限定时间，直接kill");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (IOException e) {
               throw new RuntimeException(e);
            }
        }
        return executeMessageList;
    }


    /**
     * 4、获取输出结果
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {

        //4. 收集整理输出的结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        ExecuteMessage lastexecuteMessage=executeMessageList.get(executeMessageList.size()-1);
        //如果运行时发生错误如：栈溢出、空指针、超出最大时间限制
        if(StrUtil.isNotEmpty(lastexecuteMessage.getErrorMessage())){
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setTime(lastexecuteMessage.getTime());
            judgeInfo.setMemory(lastexecuteMessage.getMemory());
            judgeInfo.setMessage(lastexecuteMessage.getErrorMessage());
            return ExecuteCodeResponse.builder()
                    .status(ExecuteCodeStatusEnum.RUN_FAILED.getValue())
                    .judgeInfo(judgeInfo)
                    .build();
        }

        //下面的情况是在运行是没有报错

        // 取用时最大值，便于判断是否超时
        long maxTime = -1;
        for (ExecuteMessage executeMessage : executeMessageList) {
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        // 正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(ExecuteCodeStatusEnum.SUCCESS.getValue());
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);

        // todo 获取java执行提交代码占用的内存要借助第三方库来获取内存占用，非常麻烦，此处不做实现
         judgeInfo.setMemory(2L);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     *  5. 文件清理
     * @param userCodeFile
     * @return
     */

    public boolean deleteFile(File userCodeFile){
        //将已经运行完的文件清理,防止服务器空间不足
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del == true ? "成功" : "失败"));
            return del;
        }
        return true;
    }


    /**
     * 获取沙箱 错误响应
     *
     * @param e
     * @return
     */

//    private ExecuteCodeResponse getErrorResponse(Throwable e) {
//        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
//        executeCodeResponse.setOutputList(new ArrayList<>());
//        executeCodeResponse.setMessage(e.getMessage());
//        //status==2表示沙箱本身发生异常，status==3表示代码本身执行有异常
//        executeCodeResponse.setStatus(2);
//        executeCodeResponse.setJudgeInfo(new JudgeInfo());
//        return executeCodeResponse;
//    }

}
