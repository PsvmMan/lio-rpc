package com.gt.lio.protocol;

import java.util.List;

public interface ProtocolCodec {

    // 编码：对象 -> 字节流（含协议头）
    byte[] encode(ProtocolMessage message) throws Exception;

    // 解码：字节流 -> 对象（含协议头）
    int decode(byte[] data, List<Object> out) throws Exception;
}
