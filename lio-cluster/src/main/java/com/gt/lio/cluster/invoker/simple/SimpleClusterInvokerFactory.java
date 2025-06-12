package com.gt.lio.cluster.invoker.simple;

import com.gt.lio.cluster.directory.ServiceDirectory;
import com.gt.lio.cluster.invoker.ClusterInvoker;
import com.gt.lio.cluster.invoker.ClusterInvokerFactory;
import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.config.model.LioReferenceMethodMetadata;

import java.util.Map;

@SPIService("simple")
public class SimpleClusterInvokerFactory implements ClusterInvokerFactory {

    @Override
    public ClusterInvoker createInvoker(ServiceDirectory serviceDirectory, Map<String, LioReferenceMethodMetadata> methods) {
        return new SimpleClusterInvoker(serviceDirectory, methods);
    }
}
