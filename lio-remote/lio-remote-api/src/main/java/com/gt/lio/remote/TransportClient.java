package com.gt.lio.remote;

import com.gt.lio.protocol.ProtocolMessage;

public interface TransportClient {
    void connect(String host, int port);

    void send(ProtocolMessage data);
    void close();
    String getLocalAddress();

    String getRemoteAddress();

    boolean isAvailable();

    boolean isClosed();
}
