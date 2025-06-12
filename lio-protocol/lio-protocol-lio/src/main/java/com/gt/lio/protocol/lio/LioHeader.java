package com.gt.lio.protocol.lio;

import com.gt.lio.protocol.header.ProtocolHeader;

import static com.gt.lio.protocol.ProtocolConstants.*;

/**
 +------------------+-----------------+-------------------+-------------------+----------------------+------------------+
 |      魔数        |   控制字段1     |    控制字段2      |    请求序号       |    数据长度          |                  |
 |    (2 字节)      |    (1 字节)     |    (1 字节)       |    (8 字节)       |    (4 字节)          |                  |
 +------------------+-----------------+-------------------+-------------------+----------------------+------------------+
 | 16 bit           | 8 bit           | 8 bit             | 64 bit            | 32 bit               |                  |
 | [Magic Number]   | [Control Byte]  | [MetaData Byte]  | [Request ID]      | [Payload Length]     |                  |
 | 字母 L (8bit)     | 消息类型(1bit) | 线程池名称编码(3bit) | 唯一请求ID        | payload字节长度      |                  |
 | 主版本 (4bit)     | 是否响应(1bit) |                   |                   |                      |                  |
 | 子版本 (4bit)     | 序列化方式(3bit)| 保留位(5bit)     |                   |                      |                  |
 |                  | 压缩方式(3bit)  |                   |                   |                      |                  |
 +------------------+-----------------+-------------------+-------------------+----------------------+------------------+
 */
public class LioHeader implements ProtocolHeader{

    // 魔数版本信息
    private short magic;

    // 控制字段：消息类型(1bit) | 是否响应(1bit) | 序列化方式(3bit) | 压缩方式(3bit)
    private byte controlByte;

    // 元数据字节：线程池名称编码(3bit) | 保留位(5bit)
    private byte metaDataByte;


    // 请求序号
    private long requestId;

    // 数据长度
    private int dataLength;

    // 构造函数（通过各个字段构造）
    public LioHeader(short magic, byte msgType, boolean isRespond, byte serializationType,
                     boolean isCompressed, byte compressionType,
                     long requestId, int dataLength) {
        this.magic = magic;
        this.requestId = requestId;
        this.dataLength = dataLength;

        this.controlByte = buildControlByte(msgType, isRespond, serializationType, isCompressed, compressionType);
    }

    public static byte buildControlByte(byte msgType, boolean isRespond, byte serializationType, boolean isCompressed, byte compressionType){
        byte b = 0;

        // 消息类型
        b |= (msgType & 0x01) << 7;

        // 需要响应才赋值，因为不响应默认值就是0
        if(isRespond){
            b |= (SHOULD_RESPOND & 0x01) << 6;
        }

        // 序列化方式
        b |= (serializationType & 0x07) << 3;

        // 需要压缩才赋值，因为不压缩默认值就是0
        if(isCompressed){
            b |= (compressionType & 0x07);
        }
        return b;
    }

    public static byte buildMetaDataByte(byte threadPoolName){
        return (byte) ((threadPoolName & 0x07) << 5);
    }

    public LioHeader (short magic, byte controlByte, byte metaDataByte, long requestId, int dataLength){
        this.magic = magic;
        this.controlByte = controlByte;
        this.metaDataByte = metaDataByte;
        this.requestId = requestId;
        this.dataLength = dataLength;
    }

    @Override
    public byte getMsgType() {
        return (byte) ((controlByte >> 7) & 0x01);
    }

    @Override
    public boolean isRespond() {
        return (controlByte >> 6 & 0x01) == SHOULD_RESPOND;
    }

    @Override
    public byte getSerializationType() {
        return (byte) ((controlByte >> 3) & 0x07);
    }

    @Override
    public boolean isCompressed() {
        return (controlByte & 0x07) != NO_COMPRESSED;
    }

    @Override
    public byte getCompressionType() {
        return (byte) (controlByte & 0x07);
    }

    @Override
    public long getRequestId() { return requestId; }

    @Override
    public byte getThreadPoolName() {
        return (byte) ((metaDataByte >> 5) & 0x07);
    }

    @Override
    public void setCompressionType(byte compressionType) {
        this.controlByte = buildControlByte(getMsgType(), isRespond(), getSerializationType(), compressionType != NO_COMPRESSED, compressionType);
    }

    @Override
    public String toString() {
        return "LioHeader{" +
                "magic=" + Integer.toHexString(magic & 0xFFFF) +
                ", msgType=" + getMsgType() +
                ", isRespond=" + isRespond() +
                ", serializationType=" + getSerializationType() +
                ", isCompressed=" + isCompressed() +
                ", compressionType=" + getCompressionType() +
                ", requestId=" + requestId +
                ", dataLength=" + dataLength +
                '}';
    }
}
