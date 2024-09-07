package com.hew.hewojcodesandbox;


import com.hew.hewojcodesandbox.model.ExecuteCodeRequest;
import com.hew.hewojcodesandbox.model.ExecuteCodeResponse;

import java.io.IOException;

public interface Codesandbox {
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) throws IOException;
}
