package com.hew.hewojcodesandbox;


import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.hew.hewojcodesandbox.model.ExecuteCodeRequest;
import com.hew.hewojcodesandbox.model.ExecuteCodeResponse;
import com.hew.hewojcodesandbox.model.ExecuteMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Component
public class JavaDockerCodesandbox extends JavaCodeSandboxTemplate {

    private static final long TIME_OUT = 5000L;
    private static final Boolean FIRST_INIT = true;


//    public static void main(String[] args) {
//        JavaDockerCodesandbox javaDockerCodesandbox = new JavaDockerCodesandbox();
//        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
//        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
//        //读文件
//        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
//        //String code = ResourceUtil.readStr("testCode/simpleCompute/Main.java", StandardCharsets.UTF_8);
//        executeCodeRequest.setCode(code);
//        executeCodeRequest.setLanguage("java");
//        ExecuteCodeResponse executeCodeResponse = javaDockerCodesandbox.executeCode(executeCodeRequest);
//        System.out.println(executeCodeResponse);
//    }

    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        String image = "openjdk:8-alpine";
        // 获取默认的 Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        //保证只下载一次镜像
//        if (FIRST_INIT) {
//            //拉取镜像
//            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
//                @Override
//                public void onNext(PullResponseItem item) {
//                    System.out.println("下载镜像：" + item.getStatus());
//                    super.onNext(item);
//                }
//            };
//            try {
//                pullImageCmd
//                        .exec(pullImageResultCallback)
//                        .awaitCompletion();
//            } catch (InterruptedException e) {
//                System.out.println("拉取镜像异常");
//                throw new RuntimeException(e);
//            }
//            System.out.println("下载完成");
//        }


        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        //通过hostconfig限制内存
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        //使用withSecurityOpts方法可以设置容器的安全选项（security options）。其中，seccomp选项允许通过指定一个安全管理配置字符串来限制容器的系统调用。
//        hostConfig.withSecurityOpts(Arrays.asList("seccomp=安全管理配置字符串"));
        //创建容器时，可以指定文件路径（Volumn） 映射，作用是把本地的文件同步到容器中，可以让容器访问。
        //也可以叫容器挂载目录
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        CreateContainerResponse createContainerResponse = containerCmd
                .withNetworkDisabled(true)  //限制网络资源
                .withHostConfig(hostConfig)
                .withReadonlyRootfs(true)//限制用户不能向root根目录写文件
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withTty(true)   //创建一个交互终端
                .exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();

        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();


        //3. 运行提交代码并返回结果
        // docker exec keen_blackwell java -cp /app Main 1 3
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            long time = 0L;
            StopWatch stopWatch = new StopWatch();
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令：" + execCreateCmdResponse);
            String execId = execCreateCmdResponse.getId();

            //提交的代码执行完后执行的回调函数，用于封装执行结果
            final String[] message = {null};
            final String[] errorMessage = {null};
            final boolean[] timeout = {true};
            //定义docker执行期间的回调(类似与对docker执行期间的监控)
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                //当程序执行完成后会执行该方法
                public void onComplete() {
                    // 如果执行完成，则表示没超时
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            final long[] maxMemory = {0L};

            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            //相当于定义一个监控容器内存占用的回调函数
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {

                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            });
            //启动对docker内存占用的监控
            statsCmd.exec(statisticsResultCallback);
            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                statsCmd.close();


            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            ExecuteMessage executeMessage = new ExecuteMessage();
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setMessage(message[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);

            //获取占用内存
        }
        return executeMessageList;
    }
}
