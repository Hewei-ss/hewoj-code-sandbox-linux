package com.hew.hewojcodesandbox;

import com.hew.hewojcodesandbox.model.ExecuteCodeRequest;
import com.hew.hewojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Java 原生代码沙箱实现（直接复用模板方法）
 */
@Component
public class JavaNativeCodesandbox extends JavaCodeSandboxTemplate {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) throws IOException {
        return super.executeCode(executeCodeRequest);
    }
}
