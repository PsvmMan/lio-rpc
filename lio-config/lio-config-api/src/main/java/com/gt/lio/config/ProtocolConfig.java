package com.gt.lio.config;

/**
 * 协议配置
 */
public class ProtocolConfig {

    // 协议名称
    private String name;

    // 服务地址
    private String host;

    // 服务端口
    private Integer port;

    // 序列化方式
    private String serialization;

    // 传输方式
    private String remote;

    // 如果在这个时间内没有收到任何数据, 则认为连接可能已经断开或对方不可达
    private Long heartbeatReadTimeout;

    // 客户端期望的心跳消息发送的最大延迟
    private Long heartbeatWriteTimeout;

    public Long getHeartbeatReadTimeout() {
        return heartbeatReadTimeout;
    }

    public void setHeartbeatReadTimeout(Long heartbeatReadTimeout) {
        this.heartbeatReadTimeout = heartbeatReadTimeout;
    }

    public Long getHeartbeatWriteTimeout() {
        return heartbeatWriteTimeout;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public void setHeartbeatWriteTimeout(Long heartbeatWriteTimeout) {
        this.heartbeatWriteTimeout = heartbeatWriteTimeout;
    }
}
