package com.gt.lio.common.invoker;

import java.util.HashMap;

public interface RpcInvokerFactory {
    RpcInvoker createInvoker(HashMap metadata, Object target);
}
