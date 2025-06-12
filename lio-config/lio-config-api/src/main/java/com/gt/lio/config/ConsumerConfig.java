package com.gt.lio.config;

import java.io.Serializable;

/**
 * 消费者配置
 */
public class ConsumerConfig {

    // 重试次数
    private Integer retries;

    // 并发数
    private Integer parallelNumber;

    // 超时时间
    private Long timeout;

    // 负载均衡
    private String loadbalance;

    // 集群类型
    private String cluster;

    // 客户端连接数
    private Integer connections;

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public Integer getParallelNumber() {
        return parallelNumber;
    }

    public void setParallelNumber(Integer parallelNumber) {
        this.parallelNumber = parallelNumber;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public Integer getConnections() {
        return connections;
    }

    public void setConnections(Integer connections) {
        this.connections = connections;
    }
}
