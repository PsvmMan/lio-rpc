package com.gt.lio.remote.netty.client;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.gt.lio.remote.netty.NettyConstants.DEFAULT_THREADS;

public class ResponseProcessor {
    private static ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(
                    DEFAULT_THREADS,
                    DEFAULT_THREADS,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(),
                    new ThreadPoolExecutor.CallerRunsPolicy());

    public static void submitRequest(Runnable task) {
        threadPoolExecutor.submit(task);
    }
}
