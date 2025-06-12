package com.gt.lio.config;

/**
 * 服务生产者配置
 */
public class ProviderConfig {

    // 是否导出服务
    private Boolean export = true;

    // 服务代理类型  框架支持jdk、cglib两种方式
    private String proxy;

    // 默认业务线程池的最大线程数
    private Integer defaultThreadPoolMaxSize;

    // 默业务线程池的核心线程数
    private Integer defaultThreadPoolCoreSize;

    // 默认业务线程池的线程最大空闲时间
    private Integer defaultThreadPoolKeepAliveTime;

    // 默认业务线程池的队列大小
    private Integer defaultThreadPoolQueueSize;

    public Boolean getExport() {
        return export;
    }

    public void setExport(Boolean export) {
        this.export = export;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public Integer getDefaultThreadPoolMaxSize() {
        return defaultThreadPoolMaxSize;
    }

    public void setDefaultThreadPoolMaxSize(Integer defaultThreadPoolMaxSize) {
        this.defaultThreadPoolMaxSize = defaultThreadPoolMaxSize;
    }

    public Integer getDefaultThreadPoolCoreSize() {
        return defaultThreadPoolCoreSize;
    }

    public void setDefaultThreadPoolCoreSize(Integer defaultThreadPoolCoreSize) {
        this.defaultThreadPoolCoreSize = defaultThreadPoolCoreSize;
    }

    public Integer getDefaultThreadPoolKeepAliveTime() {
        return defaultThreadPoolKeepAliveTime;
    }

    public void setDefaultThreadPoolKeepAliveTime(Integer defaultThreadPoolKeepAliveTime) {
        this.defaultThreadPoolKeepAliveTime = defaultThreadPoolKeepAliveTime;
    }

    public Integer getDefaultThreadPoolQueueSize() {
        return defaultThreadPoolQueueSize;
    }

    public void setDefaultThreadPoolQueueSize(Integer defaultThreadPoolQueueSize) {
        this.defaultThreadPoolQueueSize = defaultThreadPoolQueueSize;
    }
}
