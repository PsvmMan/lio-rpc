package com.gt.lio.config;

/**
 * 注册中心配置
 */
public class RegistryConfig {

    // 注册中心类型
    private String type;

    // 注册中心地址
    private String address;

    // 登录注册中心的用户名
    private String username;

    // 登录注册中心的密码
    private String password;

    // 注册中心地址的主机名
    private String host;

    // 注册中心地址的端口
    private Integer port;

    // 连接超时时间（毫秒）
    private Integer connectionTimeoutMs;

    // 会话超时时间（毫秒）
    private Integer sessionTimeoutMs;

    // 重试间隔（毫秒）
    private Integer retryIntervalMs;

    // 最大重试次数
    private Integer maxRetries;

    public Integer getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(Integer connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public Integer getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public void setSessionTimeoutMs(Integer sessionTimeoutMs) {
        this.sessionTimeoutMs = sessionTimeoutMs;
    }

    public Integer getRetryIntervalMs() {
        return retryIntervalMs;
    }

    public void setRetryIntervalMs(Integer retryIntervalMs) {
        this.retryIntervalMs = retryIntervalMs;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getAuthority() {
        if ((username == null || username.length() == 0)
                && (password == null || password.length() == 0)) {
            return null;
        }
        return (username == null ? "" : username)
                + ":" + (password == null ? "" : password);
    }

    public RegistryConfig clone() {
        RegistryConfig config = new RegistryConfig();
        config.setType(type);
        config.setAddress(address);
        config.setUsername(username);
        config.setPassword(password);
        config.setHost(host);
        config.setPort(port);
        config.setConnectionTimeoutMs(connectionTimeoutMs);
        config.setSessionTimeoutMs(sessionTimeoutMs);
        config.setRetryIntervalMs(retryIntervalMs);
        config.setMaxRetries(maxRetries);
        return config;
    }

}
