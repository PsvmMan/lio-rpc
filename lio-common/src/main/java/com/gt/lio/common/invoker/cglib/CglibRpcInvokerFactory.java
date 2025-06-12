package com.gt.lio.common.invoker.cglib;

import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.common.invoker.RpcInvokerFactory;
import com.gt.lio.common.invoker.RpcInvoker;

import java.util.HashMap;

@SPIService("cglib")
public class CglibRpcInvokerFactory implements RpcInvokerFactory {

    @Override
    public RpcInvoker createInvoker(HashMap metadata, Object target) {
        return new CglibRpcInvoker(metadata,target);
    }

}
