package com.gt.lio.common.constants;


public class ThreadPoolConstants {

    public static final String defaultThreadPoolName = "default";

    public static final int defaultThreadSize = Runtime.getRuntime().availableProcessors() * 2;

    public static final int defaultKeepAliveTime = 60;

    public static final int defaultThreadPoolQueueSize = 10000;

}
