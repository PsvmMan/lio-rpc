package com.gt.lio.protocol;

import com.gt.lio.protocol.ProtocolMessage;
import com.gt.lio.protocol.header.DefaultProtocolHeader;

public class ProtocolConstants {

    // 心跳消息
    public static final byte HEARTBEAT_MESSAGE = 0x01;

    // 业务消息
    public static final byte BUSINESS_MESSAGE = 0x00;

    // 当压缩类型为0的时候，代表不压缩
    public static final byte NO_COMPRESSED = 0x00;

    // 不需要响应
    public static final byte SHOULD_RESPOND = 0x01;


    public static final ProtocolMessage HEARTBEAT_DATA  = new ProtocolMessage();

    static {
        HEARTBEAT_DATA.setHeader(new DefaultProtocolHeader(HEARTBEAT_MESSAGE,false, (byte) 0, false, (byte) 0, (byte) 0, 0));
    }
}
