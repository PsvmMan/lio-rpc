package com.gt.lio.common.invoker.jdk;

import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.common.invoker.RpcInvokerFactory;
import com.gt.lio.common.invoker.RpcInvoker;

import java.util.HashMap;

@SPIService("jdk")
public class JdkRpcInvokerFactory implements RpcInvokerFactory {

    @Override
    public RpcInvoker createInvoker(HashMap metadata, Object target) {
        return new JdkRpcInvoker(metadata, target);
    }
}
