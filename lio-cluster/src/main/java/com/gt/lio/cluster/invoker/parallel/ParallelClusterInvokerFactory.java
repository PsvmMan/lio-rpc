package com.gt.lio.cluster.invoker.parallel;

import com.gt.lio.cluster.directory.ServiceDirectory;
import com.gt.lio.cluster.invoker.ClusterInvoker;
import com.gt.lio.cluster.invoker.ClusterInvokerFactory;
import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.config.model.LioReferenceMethodMetadata;

import java.util.Map;

@SPIService("parallel")
public class ParallelClusterInvokerFactory implements ClusterInvokerFactory {

    @Override
    public ClusterInvoker createInvoker(ServiceDirectory serviceDirectory, Map<String, LioReferenceMethodMetadata> methods) {
        return new ParallelClusterInvoker(serviceDirectory, methods);
    }
}
