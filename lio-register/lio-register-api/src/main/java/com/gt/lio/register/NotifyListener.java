package com.gt.lio.register;

import java.util.List;

public interface NotifyListener {
    void notify(List<ServiceInstance> instances,String registerKey);
}
