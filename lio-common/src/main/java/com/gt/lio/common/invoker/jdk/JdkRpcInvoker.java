package com.gt.lio.common.invoker.jdk;

import com.gt.lio.common.invoker.RpcInvoker;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.gt.lio.common.utils.CommonUtils.generateMethodKey;

public class JdkRpcInvoker implements RpcInvoker {

    // 这是目标对象
    private final Object target;

    private final HashMap metadata;
    private final ConcurrentMap<String, Method> methodCache = new ConcurrentHashMap<>();

    public JdkRpcInvoker(HashMap metadata, Object proxy) {
        this.target = proxy;
        this.metadata = metadata;
    }

    @Override
    public Object invoke(String methodName, Object[] args, Class<?>... paramTypes) throws Exception {
        String methodKey = generateMethodKey(methodName, paramTypes);
        Method method = methodCache.get(methodKey);
        if(method == null){
            methodCache.putIfAbsent(methodKey,findMethod(methodName,paramTypes));
            method = methodCache.get(methodKey);
        }
        return method.invoke(target, args);
    }

    @Override
    public HashMap getMetadata() {
        return metadata;
    }

    private Method findMethod(String methodName, Class<?>[] paramTypes) throws NoSuchMethodException {
        return target.getClass().getMethod(methodName, paramTypes);
    }
}
