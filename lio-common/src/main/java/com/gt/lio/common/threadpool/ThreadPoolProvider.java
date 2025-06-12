package com.gt.lio.common.threadpool;

import com.gt.lio.common.spi.LioServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolProvider {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolProvider.class);
    
    public static final ThreadPoolProvider instance = new ThreadPoolProvider();

    private  Map<String, ThreadPoolExecutor> threadPoolCache = new ConcurrentHashMap<>();
    
    private ThreadPoolProvider(){}

    public void buildThreadPool(String threadPoolName, Map<String,Integer> params) {
        threadPoolCache.computeIfAbsent(threadPoolName, k -> {
            ThreadPoolFactory threadPoolFactory = LioServiceLoader.getServiceLoader(ThreadPoolFactory.class).getService(threadPoolName);
            ThreadPoolExecutor threadPool = threadPoolFactory.createThreadPool(params);
            if(logger.isInfoEnabled()){
                logger.info("thread pool {} created", threadPoolName);
            }
            return threadPool;
        });
    }

    public ThreadPoolExecutor getThreadPool(String threadPoolName) {
        return threadPoolCache.get(threadPoolName);
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ThreadPoolProvider.instance.threadPoolCache.forEach((k, v) -> {
                if(v != null){
                    v.shutdown();
                    if(logger.isInfoEnabled()){
                        logger.info("thread pool {} shutdown", k);
                    }
                }
            });
        }));
    }
    
}
