package com.gt.lio.cluster.invoker.simple;

import com.gt.lio.cluster.client.ClientInvoker;
import com.gt.lio.cluster.directory.ServiceDirectory;
import com.gt.lio.cluster.invoker.AbstractClusterInvoker;
import com.gt.lio.cluster.loadbalance.LoadBalance;
import com.gt.lio.config.model.LioReferenceMethodMetadata;
import com.gt.lio.protocol.body.RequestMessage;
import com.gt.lio.protocol.body.ResponseMessage;

import java.util.List;
import java.util.Map;

/**
 * 简单集群调用，报错直接返回异常
 */
public class SimpleClusterInvoker extends AbstractClusterInvoker {

    public SimpleClusterInvoker(ServiceDirectory serviceDirectory, Map<String, LioReferenceMethodMetadata> methods){
        super(serviceDirectory, methods);
    }

    @Override
    public ResponseMessage invoke(RequestMessage req, List<ClientInvoker> clientInvokers, LoadBalance loadBalance, LioReferenceMethodMetadata methodMetadata) {
        try {
            ClientInvoker clientInvoker = loadBalance.select(clientInvokers, req);
            if (clientInvoker == null) {
                return new ResponseMessage(new RuntimeException("没有可用服务节点"));
            }
            return clientInvoker.invoke(req);
        }
        catch (Throwable e) {
            return new ResponseMessage(e);
        }
    }
}
