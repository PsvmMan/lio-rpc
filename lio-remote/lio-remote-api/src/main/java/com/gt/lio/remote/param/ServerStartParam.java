package com.gt.lio.remote.param;

public class ServerStartParam {

    /**
     * 服务器主机地址
     */
    private String host;

    /**
     * 服务器端口
     */
    private Integer port;

    /**
     * 服务器会话超时时间
     */
    private Long heartbeatReadTimeout;

    /**
     * 网络协议
     */
    private String protocol;

    public ServerStartParam(String host, Integer port, Long heartbeatReadTimeout, String protocol) {
        this.host = host;
        this.port = port;
        this.heartbeatReadTimeout = heartbeatReadTimeout;
        this.protocol = protocol;
    }

    public ServerStartParam() {
    }


    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
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

    public Long getHeartbeatReadTimeout() {
        return heartbeatReadTimeout;
    }

    public void setHeartbeatReadTimeout(Long heartbeatReadTimeout) {
        this.heartbeatReadTimeout = heartbeatReadTimeout;
    }
}
