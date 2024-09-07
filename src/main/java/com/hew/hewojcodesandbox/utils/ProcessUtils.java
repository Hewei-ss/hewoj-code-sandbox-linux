package com.hew.hewojcodesandbox.utils;


import cn.hutool.core.util.StrUtil;
import com.hew.hewojcodesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程工具类
 */
public class ProcessUtils {


    /**
     * 等待执行进程完成并获取返回结果
     *
     * @param runProcess
     * @param opName
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch=new StopWatch();
            stopWatch.start();
            //等待执行提交代码或则编译完成后...获取退出值
            int exitValue = runProcess.waitFor();
            //组装结果并返回
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) {
                System.out.println(opName + "成功");
                List<String> outputStrList=new ArrayList<>();
                //分批获取进程的正常输出
                //将输出的字节流流信息转换为字符流信息，在存入BufferedReader缓冲流中
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                String compliOutputLine;
                //逐行读取
                while ((compliOutputLine = bufferedReader.readLine()) != null) {
                    outputStrList.add(compliOutputLine);
                }
                executeMessage.setMessage(StringUtils.join(outputStrList, "\n"));
            } else {
                System.out.println(opName + "失败" + exitValue);
                executeMessage.setExitValue(exitValue);
                List<String> outputStrList=new ArrayList<>();
                //分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                String compliOutputLine;
                while ((compliOutputLine = bufferedReader.readLine()) != null) {
                    outputStrList.add(compliOutputLine);
                }
                executeMessage.setMessage(StringUtils.join(outputStrList, "\n"));
                //分批获取进程的异常输出
                BufferedReader errorbufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                List<String> errorOutputStrList = new ArrayList<>();
                String errorcompliOutputLine;
                while ((errorcompliOutputLine = errorbufferedReader.readLine()) != null) {
                    errorOutputStrList.add(errorcompliOutputLine);
                }
                executeMessage.setErrorMessage(StringUtils.join(errorOutputStrList, "\n"));
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return executeMessage;
    }


    /**
     * 执行交互式进程并获取信息
     *
     * @param runProcess
     * @param opName
     * @return
     */


    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String opName, String input) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            //通过该输出流 runProcess.getOutputStream();可以将数据传输到子进程runProcess的标准输入中。
            //将输入通过runProcess.getOutputStream();返回的流，输入到提交程序中的Scanner输入中。
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = input.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            outputStreamWriter.flush();


            //分批次获取进程正常输出
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
            StringBuilder compileOutputBuilder = new StringBuilder();
            String compliOutputLine;
            //逐行读取
            while ((compliOutputLine = bufferedReader.readLine()) != null) {
                compileOutputBuilder.append(compliOutputLine);
            }
            executeMessage.setMessage(compileOutputBuilder.toString());

            //资源回收释放
            outputStreamWriter.close();
            outputStream.close();
            runProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }


    /**
     * 执行交互式进程并获取信息
     * @param runProcess
     * @param input
     * @return
     */
    public static ExecuteMessage getAcmProcessMessage(Process runProcess, String input) throws IOException {
        ExecuteMessage executeMessage = new ExecuteMessage();

        StringReader inputReader = new StringReader(input);
        BufferedReader inputBufferedReader = new BufferedReader(inputReader);

        //计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        //输入（模拟控制台输入）
        PrintWriter consoleInput = new PrintWriter(runProcess.getOutputStream());
        String line;
        while ((line = inputBufferedReader.readLine()) != null) {
            consoleInput.println(line);
            consoleInput.flush();
        }
        consoleInput.close();

        //获取输出
        BufferedReader userCodeOutput = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
        List<String> outputList = new ArrayList<>();
        String outputLine;
        while ((outputLine = userCodeOutput.readLine()) != null) {
            outputList.add(outputLine);
        }
        userCodeOutput.close();

        //获取错误输出
        BufferedReader errorOutput = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
        List<String> errorList = new ArrayList<>();
        String errorLine;
        while ((errorLine = errorOutput.readLine()) != null) {
            errorList.add(errorLine);
        }
        errorOutput.close();

        stopWatch.stop();
        executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        //设置一个消耗内存假数据
        executeMessage.setMemory(2L);
        executeMessage.setMessage(StringUtils.join(outputList, "\n"));
        executeMessage.setErrorMessage(StringUtils.join(errorList, "\n"));
        runProcess.destroy();

        return executeMessage;
    }

    /**
     * 获取某个流的输出
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static String getProcessOutput(InputStream inputStream) throws IOException {
        // 分批获取进程的正常输出
        // Linux写法
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        //Windows写法
        // BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "GBK"));
        StringBuilder outputSb = new StringBuilder();
        // 逐行读取
        String outputLine;
        while ((outputLine = bufferedReader.readLine()) != null) {
            outputSb.append(outputLine).append("\n");
        }
        bufferedReader.close();
        return outputSb.toString();
    }
}
