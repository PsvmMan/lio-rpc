package com.gt.lio.protocol.header;

public interface ProtocolHeader {


    /**
     * 获取消息类型，0 业务消息 | 1 心跳消息
     * @return
     */
    byte getMsgType();

    /**
     * 获取是否响应，0 不响应 | 1 响应
     * @return
     */
    boolean isRespond();

    /**
     * 获取序列化方式
     * @return
     */
    byte getSerializationType();

    /**
     * 获取是否压缩，0 不压缩 | 1 压缩
     * @return
     */
    boolean isCompressed();

    /**
     * 获取压缩方式
     * @return
     */
    byte getCompressionType();

    byte getThreadPoolName();

    /**
     * 获取请求id
     * @return
     */
    long getRequestId();

    void setCompressionType(byte compressionType);


}
