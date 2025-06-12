package com.gt.lio.protocol;

import com.gt.lio.protocol.header.ProtocolHeader;

public class ProtocolMessage {

    ProtocolHeader header;

    private byte[] body;

    public ProtocolMessage() {
    }

    public ProtocolMessage(ProtocolHeader header, byte[] body) {
        this.header = header;
        this.body = body;
    }

    public ProtocolHeader getHeader() {
        return header;
    }

    public void setHeader(ProtocolHeader header) {
        this.header = header;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
