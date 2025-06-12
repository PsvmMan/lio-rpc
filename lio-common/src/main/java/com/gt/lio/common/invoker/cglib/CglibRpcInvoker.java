package com.gt.lio.common.invoker.cglib;

import com.gt.lio.common.invoker.RpcInvoker;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.gt.lio.common.utils.CommonUtils.generateMethodKey;

public class CglibRpcInvoker implements RpcInvoker {

    private final Object target;

    private final FastClass fastClass;

    private final HashMap metadata;

    private final ConcurrentMap<String, FastMethod> methodCache  = new ConcurrentHashMap<>();

    public CglibRpcInvoker(HashMap metadata,Object target) {
        this.target = target;
        this.fastClass = FastClass.create(target.getClass());
        this.metadata = metadata;
    }

    @Override
    public Object invoke(String methodName, Object[] args, Class<?>... paramTypes) throws Exception {
        String methodKey = generateMethodKey(methodName, paramTypes);
        FastMethod fastMethod = methodCache.get(methodKey);
        if(fastMethod == null){
            methodCache.putIfAbsent(methodKey,findMethod(methodName,paramTypes));
            fastMethod = methodCache.get(methodKey);
        }
        return fastMethod.invoke(target, args);
    }

    @Override
    public HashMap getMetadata() {
        return metadata;
    }


    private FastMethod findMethod(String methodName, Class<?>[] paramTypes) throws NoSuchMethodException {
        Method method = target.getClass().getMethod(methodName, paramTypes);
        return fastClass.getMethod(method);
    }

}
