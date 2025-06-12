package com.gt.lio.common.threadpool;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public interface ThreadPoolFactory {
    ThreadPoolExecutor createThreadPool(Map<String,Integer> params);

}
