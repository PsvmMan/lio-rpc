package com.gt.lio.common.threadpool;

import com.gt.lio.common.annotation.SPIService;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.gt.lio.common.constants.ThreadPoolConstants.*;

@SPIService(value = defaultThreadPoolName, code = 0x01)
public class DefaultThreadPoolFactory implements ThreadPoolFactory {
    @Override
    public ThreadPoolExecutor createThreadPool(Map<String,Integer> params) {
        // 默认线程池的参数会从ProvideConfig中获取
        // 客户自定义的线程池重写createThreadPool方法时，可以忽略掉这些参数
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                params.get("threadPoolCoreSize") == null ? defaultThreadSize : params.get("threadPoolCoreSize"),
                params.get("threadPoolMaxSize") == null ? defaultThreadSize : params.get("threadPoolMaxSize"),
                params.get("threadPoolKeepAliveTime") == null ? defaultKeepAliveTime : params.get("threadPoolKeepAliveTime"),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(params.get("threadPoolQueueSize") == null ? defaultThreadPoolQueueSize : params.get("threadPoolQueueSize")),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        return executor;
    }
}
