package com.gt.lio.remote.netty;

public class NettyConstants {

    public static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors() * 2;

    public static final long READ_IDLE_TIME = 60 * 1000L;

    public static final long WRITER_IDLE_TIME = 45 * 1000L;

    // 最大重连次数
    public static final int MAX_RECONNECT_TIMES = 3;


}
