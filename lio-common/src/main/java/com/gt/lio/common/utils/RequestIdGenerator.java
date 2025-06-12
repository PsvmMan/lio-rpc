package com.gt.lio.common.utils;

import sun.misc.Contended;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class RequestIdGenerator {

    public static final String DEFAULT_KEY = "*";

    private static final ConcurrentHashMap<String, AtomicLong> counterMap = new ConcurrentHashMap<>();

    public static long nextId(String key) {
        AtomicLong atomicLong = counterMap.get(key);
        if (atomicLong == null) {
            atomicLong = counterMap.computeIfAbsent(key, k -> new AtomicLong());
        }
        return atomicLong.incrementAndGet();
    }

}
