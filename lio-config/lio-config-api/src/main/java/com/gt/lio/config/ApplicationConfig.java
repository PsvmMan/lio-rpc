package com.gt.lio.config;

/**
 * 应用配置
 */
public class ApplicationConfig {

    // 应用名称
    private String name;

    // 配置全局的版本，如果需要特别配置，可以在@LioService注解、@LioReference注解上单独配置
    private String version;

    // 配置全局的分组，如果需要特别配置，可以在@LioService注解、@LioReference注解上单独配置
    private String group;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
