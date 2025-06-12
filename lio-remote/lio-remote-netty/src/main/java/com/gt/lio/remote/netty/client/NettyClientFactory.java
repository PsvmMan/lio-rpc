package com.gt.lio.remote.netty.client;

import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.remote.ClientFactory;
import com.gt.lio.remote.param.ClientStartParam;
import com.gt.lio.remote.TransportClient;

@SPIService("netty")
public class NettyClientFactory implements ClientFactory {

    @Override
    public TransportClient createClient(ClientStartParam param) {
        NettyClient nettyClient = new NettyClient(param);
        nettyClient.connect(param.getHost(), param.getPort());
        return nettyClient;
    }
}
