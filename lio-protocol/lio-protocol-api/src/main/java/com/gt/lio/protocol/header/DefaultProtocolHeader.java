package com.gt.lio.protocol.header;

import com.gt.lio.protocol.header.ProtocolHeader;

public class DefaultProtocolHeader implements ProtocolHeader {
    private  byte msgType;
    private  boolean isRespond;
    private  byte serializationType;
    private  boolean isCompressed;
    private  byte compressionType;
    private  byte threadPoolName;
    private  long requestId;

    public DefaultProtocolHeader(byte msgType, boolean isRespond, byte serializationType,
                                 boolean isCompressed, byte compressionType,
                                 byte threadPoolName, long requestId) {
        this.msgType = msgType;
        this.isRespond = isRespond;
        this.serializationType = serializationType;
        this.isCompressed = isCompressed;
        this.compressionType = compressionType;
        this.threadPoolName = threadPoolName;
        this.requestId = requestId;
    }

    @Override
    public byte getMsgType() {
        return msgType;
    }

    @Override
    public boolean isRespond() {
        return isRespond;
    }

    @Override
    public byte getSerializationType() {
        return serializationType;
    }

    @Override
    public boolean isCompressed() {
        return isCompressed;
    }

    @Override
    public byte getCompressionType() {
        return compressionType;
    }

    @Override
    public byte getThreadPoolName() {
        return threadPoolName;
    }

    @Override
    public long getRequestId() {
        return requestId;
    }

    @Override
    public void setCompressionType(byte compressionType) {
        this.compressionType = compressionType;
    }
}
