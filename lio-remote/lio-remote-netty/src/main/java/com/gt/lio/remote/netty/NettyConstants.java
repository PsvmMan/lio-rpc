package com.gt.lio.remote.netty;

public class NettyConstants {

    public static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors() * 2;

    public static final long READ_IDLE_TIME = 30 * 1000L;

    public static final long WRITER_IDLE_TIME = 12 * 1000L;


}
