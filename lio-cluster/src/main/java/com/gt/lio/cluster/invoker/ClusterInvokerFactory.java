package com.gt.lio.cluster.invoker;

import com.gt.lio.cluster.directory.ServiceDirectory;
import com.gt.lio.config.model.LioReferenceMethodMetadata;

import java.util.Map;

public interface ClusterInvokerFactory {

    ClusterInvoker createInvoker(ServiceDirectory serviceDirectory, Map<String, LioReferenceMethodMetadata> methods);
}
