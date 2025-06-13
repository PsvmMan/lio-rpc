package com.gt.lio.cluster.loadbalance;

import com.gt.lio.cluster.client.ClientInvoker;
import com.gt.lio.protocol.body.RequestMessage;

import java.util.List;

public abstract class AbstractLoadBalance implements LoadBalance{

    @Override
    public ClientInvoker select(List<ClientInvoker> clientInvokers, RequestMessage req) {

        // 如果没有服务提供者，则直接返回 null
        if (clientInvokers == null || clientInvokers.isEmpty()) {
            return null;
        }

        // 如果只有一个服务提供者，则直接返回
        if (clientInvokers.size() == 1) {
            return clientInvokers.get(0);
        }

        return doSelect(clientInvokers, req);
    }


    public abstract ClientInvoker doSelect(List<ClientInvoker> clientInvokers, RequestMessage req);
}
