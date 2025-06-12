package com.gt.lio.cluster.loadbalance;

import com.gt.lio.cluster.client.ClientInvoker;
import com.gt.lio.protocol.body.RequestMessage;

import java.util.List;

public interface LoadBalance {

    ClientInvoker select(List<ClientInvoker> clientInvokers, RequestMessage req);
}
