package com.gt.lio.cluster.invoker;

import com.gt.lio.cluster.client.ClientInvoker;
import com.gt.lio.cluster.directory.ServiceDirectory;
import com.gt.lio.cluster.loadbalance.LoadBalance;
import com.gt.lio.common.spi.LioServiceLoader;
import com.gt.lio.config.model.LioReferenceMethodMetadata;
import com.gt.lio.protocol.body.RequestMessage;
import com.gt.lio.protocol.body.ResponseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.gt.lio.config.model.LioReferenceMethodMetadata.DEFAULT;

public abstract class AbstractClusterInvoker implements ClusterInvoker{

    private ServiceDirectory serviceDirectory;

    private Map<String, LioReferenceMethodMetadata> methods;

    public AbstractClusterInvoker(ServiceDirectory serviceDirectory, Map<String, LioReferenceMethodMetadata> methods) {
        this.serviceDirectory = serviceDirectory;
        this.methods = methods;
    }

    @Override
    public ResponseMessage invoke(RequestMessage req) {
        try {

            // 获取服务实例
            List<ClientInvoker> clientInvokerList = serviceDirectory.getClientInvokerList();
            if (clientInvokerList == null || clientInvokerList.isEmpty()) {
                throw  new RuntimeException("No provider available for service " + serviceDirectory.getServiceName());
            }

            LioReferenceMethodMetadata metadata = methods.getOrDefault(req.getMethodKey(), methods.get(DEFAULT));

            // 负载均衡
            LoadBalance loadBalance = LioServiceLoader.getServiceLoader(LoadBalance.class).getService(metadata.getLoadBalance());

            return invoke(req, clientInvokerList, loadBalance, metadata);

        }catch (Throwable e){
            return new ResponseMessage(e);
        }
    }

    public ClientInvoker select(List<ClientInvoker> clientInvokers, RequestMessage req, LoadBalance loadBalance) {

        if (clientInvokers == null || clientInvokers.isEmpty()) {
            throw  new RuntimeException("No provider available for service " + serviceDirectory.getServiceName());
        }

        // 第一次直接尝试选择
        ClientInvoker candidate = loadBalance.select(clientInvokers, req);
        if (candidate != null && candidate.isAvailable()) {
            return candidate;
        }

        // 第一次失败，开始进入重试逻辑：拷贝并剔除不可用
        List<ClientInvoker> copy = new ArrayList<>(clientInvokers);
        copy.remove(candidate); // 移除第一次失败的节点

        while (!copy.isEmpty()) {
            candidate = loadBalance.select(copy, req);
            if (candidate != null && candidate.isAvailable()) {
                return candidate;
            }
            copy.remove(candidate); // 继续剔除不可用节点
        }

        throw  new RuntimeException("No provider available for service " + serviceDirectory.getServiceName());
    }

    public abstract ResponseMessage invoke(RequestMessage req, List<ClientInvoker> clientInvokers, LoadBalance loadBalance, LioReferenceMethodMetadata methodMetadata);
}
