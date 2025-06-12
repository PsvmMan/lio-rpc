package com.gt.lio.common.invoker;

import java.util.HashMap;

public interface RpcInvoker {
    Object invoke(String methodName, Object[] args, Class<?>... paramTypes) throws Exception;

    HashMap getMetadata();
}
