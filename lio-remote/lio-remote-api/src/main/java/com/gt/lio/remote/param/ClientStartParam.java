package com.gt.lio.remote.param;


public class ClientStartParam {

    /**
     * 服务器主机地址
     */
    private String host;

    /**
     * 服务器端口
     */
    private Integer port;

    /**
     * 写超时时间
     */
    private Long heartbeatWriteTimeout;

    /**
     * 网络协议
     */
    private String protocol;


    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Long getHeartbeatWriteTimeout() {
        return heartbeatWriteTimeout;
    }

    public void setHeartbeatWriteTimeout(Long heartbeatWriteTimeout) {
        this.heartbeatWriteTimeout = heartbeatWriteTimeout;
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
}
