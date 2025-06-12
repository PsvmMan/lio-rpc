package com.gt.lio.register;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ServiceInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    // 分隔符
    public static final String METADATA_SEPARATOR = ":";

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务协议
     */
    private String protocol;

    /**
     * 服务地址
     */
    private String host;

    /**
     * 服务端口
     */
    private Integer port;

    /**
     * 服务元数据
     */
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 获取接口实例名称
     *
     * @return
     */
    public String generateInstanceName(){
        return protocol + METADATA_SEPARATOR + host + METADATA_SEPARATOR + port;
    }


    /**
     * 获取接口实例唯一key
     *
     * @return
     */
    public String generateInstanceKey() {
        return getServiceName() + METADATA_SEPARATOR + generateInstanceName();
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
