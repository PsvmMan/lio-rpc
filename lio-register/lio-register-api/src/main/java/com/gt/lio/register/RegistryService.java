package com.gt.lio.register;

import java.util.List;

public interface RegistryService {
    void register(ServiceInstance instance) ;

    void unregister(ServiceInstance instance);

    void subscribe(String serviceName, NotifyListener listener);

    void unsubscribe(String serviceName, NotifyListener listener);

    List<ServiceInstance> getInstances(String serviceName);

    void destroy();
}
