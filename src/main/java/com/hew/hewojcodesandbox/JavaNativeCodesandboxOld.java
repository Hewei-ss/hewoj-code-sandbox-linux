//package com.hew.hewojcodesandbox;
//
//import cn.hutool.core.io.FileUtil;
//import cn.hutool.core.io.resource.ResourceUtil;
//import cn.hutool.core.util.StrUtil;
//import cn.hutool.dfa.WordTree;
//import com.hew.hewojcodesandbox.model.ExecuteCodeRequest;
//import com.hew.hewojcodesandbox.model.ExecuteCodeResponse;
//import com.hew.hewojcodesandbox.model.ExecuteMessage;
//import com.hew.hewojcodesandbox.model.JudgeInfo;
//import com.hew.hewojcodesandbox.model.enums.ExecuteCodeStatusEnum;
//import com.hew.hewojcodesandbox.utils.ProcessUtils;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.UUID;
//
//public class JavaNativeCodesandboxOld implements Codesandbox {
//
//    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
//
//    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
//
//    private static final long TIME_OUT = 5000L;
//    //设置黑名单
//    private static final List<String> blackList=Arrays.asList("Files","exec");
//    public static final WordTree WORD_TREE;
//
//
//
//
//    //编译好的自定义安全管理器所在路径
//    private static final String SECURITY_MANAGER_PATH = "D:\\IDEAWorkSpace\\hewoj-code-sandbox\\src\\main\\resources\\security";
//    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";
//
//    // 初始化字典树
//    static {
//        WORD_TREE=new WordTree();
//        WORD_TREE.addWords(blackList);
//    }
//
//
//    public static void main(String[] args) {
//        JavaNativeCodesandboxOld javaNativeCodesandbox = new JavaNativeCodesandboxOld();
//        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
//        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
//        //读文件
//        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
//        //String code = ResourceUtil.readStr("testCode/simpleCompute/Main.java", StandardCharsets.UTF_8);
//        executeCodeRequest.setCode(code);
//        executeCodeRequest.setLanguage("java");
//        ExecuteCodeResponse executeCodeResponse = javaNativeCodesandbox.executeCode(executeCodeRequest);
//        System.out.println(executeCodeResponse);
//    }
//
//    @Override
//    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
//        List<String> inputList = executeCodeRequest.getInputList();
//        String code = executeCodeRequest.getCode();
//        String language = executeCodeRequest.getLanguage();
//
//        //校验代码中是否包含黑名单中的命令(有漏洞：无法在黑名单里标注所有的禁止关键词；不同的编程语言，你对应的领域、关键词都不一样，限制人工成本很大)
//        //使用字典树进行字符串匹配
////        FoundWord foundWord=WORD_TREE.matchWord(code);
////        if(foundWord!=null){
////            System.out.println("包含敏感词"+foundWord.getFoundWord());
////            return null;
////        }
//        // 1.将用户提交的代码文件保存为文件
//        String userDir = System.getProperty("user.dir");
//
//        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
//        // 判断全局代码目录是否存在，没有则新建
//        if (!FileUtil.exist(globalCodePathName)) {
//            FileUtil.mkdir(globalCodePathName);
//        }
//
//        //把用户的代码隔离存放
//        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
//        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
//        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
//
//
//        //2.编译代码得到class文件
//
//        // 定义编译的指令
//        String complicCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
//        Process compliProcess = null;
//        try {
//            //执行指令
//            compliProcess = Runtime.getRuntime().exec(complicCmd);
//            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compliProcess, "编译");
//            System.out.println(executeMessage);
//        } catch (IOException e) {
//            return getErrorResponse(e);
//        }
//
//        //3. 执行代码返回结果
//        List<ExecuteMessage> executeMessageList = new ArrayList<>();
//        for (String input : inputList) {
//            //String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, input);
//            //在要运行移交代码时，在指令中指定开启安全管理器
//            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, input);
//            try {
//                Process runProcess = Runtime.getRuntime().exec(runCmd);
//                //超时了
//                new Thread(() -> {
//                    try {
//                        Thread.sleep(TIME_OUT);
//                        System.out.println("超时了");
//                        runProcess.destroy();
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }).start();
//                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
//                //ExecuteMessage executeMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess, "运行",input);
//                executeMessageList.add(executeMessage);
//            } catch (IOException e) {
//                return getErrorResponse(e);
//            }
//        }
//
//
//        //4. 收集整理输出的结果
//        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
//        List<String> outputList = new ArrayList<>();
//        // 取用时最大值，便于判断是否超时
//        long maxTime = -1;
//        for (ExecuteMessage executeMessage : executeMessageList) {
//            if (StrUtil.isNotBlank(executeMessage.getErrorMessage())) {
//                executeCodeResponse.setMessage(executeMessage.getErrorMessage());
//                executeCodeResponse.setStatus(3);
//                break;
//            }
//            outputList.add(executeMessage.getMessage());
//            Long time = executeMessage.getTime();
//            if (time != null) {
//                maxTime = Math.max(maxTime, time);
//            }
//        }
//        // 正常运行完成
//        if (outputList.size() == executeMessageList.size()) {
//            executeCodeResponse.setStatus(ExecuteCodeStatusEnum.SUCCESS.getValue());
//        }
//        executeCodeResponse.setOutputList(outputList);
//        JudgeInfo judgeInfo = new JudgeInfo();
//        judgeInfo.setTime(maxTime);
//
//        // todo 获取java执行提交代码占用的内存要借助第三方库来获取内存占用，非常麻烦，此处不做实现
//        // judgeInfo.setMemory();
//        executeCodeResponse.setJudgeInfo(judgeInfo);
//
//
//        //将已经运行完的文件清理,防止服务器空间不足
//        if (userCodeFile.getParentFile() != null) {
//            boolean del = FileUtil.del(userCodeParentPath);
//            System.out.println("删除" + (del == true ? "成功" : "失败"));
//        }
//
//        return executeCodeResponse;
//    }
//
//
//    /**
//     * 获取沙箱 错误响应
//     *
//     * @param e
//     * @return
//     */
//
//    private ExecuteCodeResponse getErrorResponse(Throwable e) {
//        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
//        executeCodeResponse.setOutputList(new ArrayList<>());
//        executeCodeResponse.setMessage(e.getMessage());
//        //status==2表示沙箱本身发生异常，status==3表示代码本身执行有异常
//        executeCodeResponse.setStatus(2);
//        executeCodeResponse.setJudgeInfo(new JudgeInfo());
//        return executeCodeResponse;
//    }
//}
