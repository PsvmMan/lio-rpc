package com.gt.lio.cluster.directory;

import com.gt.lio.cluster.client.ClientInvoker;

import java.util.List;

public interface Directory {

    List<ClientInvoker> getClientInvokerList();
}
