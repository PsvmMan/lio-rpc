package com.gt.lio.common.invoker;

import com.gt.lio.common.spi.LioServiceLoader;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class RpcInvokerProvider {

    public final static RpcInvokerProvider instance = new RpcInvokerProvider();

    private RpcInvokerProvider(){}

    private  ConcurrentMap<String, RpcInvoker> invokerCache  = new ConcurrentHashMap<>();

    public void buildInvoker(String key, Object target, HashMap metadata, String invokerType){
        invokerCache.computeIfAbsent(key, k -> {
            RpcInvokerFactory invokerFactory = LioServiceLoader.getServiceLoader(RpcInvokerFactory.class).getService(invokerType);
            return invokerFactory.createInvoker(metadata, target);
        });
    }

    public RpcInvoker getInvoker(String key){
        return invokerCache.get(key);
    }

}
