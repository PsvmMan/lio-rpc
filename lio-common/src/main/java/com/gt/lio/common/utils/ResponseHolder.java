package com.gt.lio.common.utils;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ResponseHolder {

    // 存储 requestId -> CompletableFuture 的映射
    private static final Map<String, CompletableFuture<Object>> futureMap = new ConcurrentHashMap<>();

    private static final Map<String, AtomicInteger> atomicIntegerMap = new ConcurrentHashMap<>();

    /**
     * 创建一个 Future 并注册到 map 中
     */
    public static CompletableFuture<Object> register(String key) {
        CompletableFuture<Object> completableFuture = futureMap.get(key);
        if (completableFuture != null) {
            return completableFuture;
        }
        return futureMap.computeIfAbsent(key, k -> new CompletableFuture<>());
    }

    /**
     * 完成指定 requestId 的 Future，并移除它
     */
    public static boolean complete(String key, Object response) {
        CompletableFuture<Object> future = futureMap.remove(key);
        if (future != null && !future.isDone()) {
            future.complete(response);
            return true;
        }
        return false;
    }


    public static CompletableFuture<Object> register(String key, int count) {
        CompletableFuture<Object> completableFuture = futureMap.get(key);
        if (completableFuture != null) {
            return completableFuture;
        }
        return futureMap.computeIfAbsent(key, k -> {
            atomicIntegerMap.put(key, new AtomicInteger(count));
            return new CompletableFuture<>();
        });
    }

    public static boolean canComplete(String key) {
        AtomicInteger atomicInteger = atomicIntegerMap.get(key);
        if (atomicInteger == null) {
            return false;
        }
        return atomicInteger.decrementAndGet() == 0;
    }

}
