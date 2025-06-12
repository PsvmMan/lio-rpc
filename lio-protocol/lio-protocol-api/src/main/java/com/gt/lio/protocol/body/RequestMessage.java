package com.gt.lio.protocol.body;

import java.io.Serializable;
import java.util.Arrays;

public class RequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 方法参数
     */
    private Object[] args;

    /**
     * 方法参数类型
     */
    private Class<?>[] paramTypes;

    /**
     * 方法key
     */
    private String methodKey;

    public RequestMessage() {
    }

    public RequestMessage(String serviceName, String methodName, Object[] args, Class<?>[] paramTypes, String methodKey) {
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.args = args;
        this.paramTypes = paramTypes;
        this.methodKey = methodKey;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(Class<?>[] paramTypes) {
        this.paramTypes = paramTypes;
    }

    public String getMethodKey() {
        return methodKey;
    }

    public void setMethodKey(String methodKey) {
        this.methodKey = methodKey;
    }

    @Override
    public String toString() {
        return "RequestMessage{" +
                "serviceName='" + serviceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", args=" + Arrays.toString(args) +
                ", paramTypes=" + Arrays.toString(paramTypes) +
                '}';
    }
}
