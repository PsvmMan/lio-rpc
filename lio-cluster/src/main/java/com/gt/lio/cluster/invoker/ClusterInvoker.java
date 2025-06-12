package com.gt.lio.cluster.invoker;

import com.gt.lio.protocol.body.RequestMessage;
import com.gt.lio.protocol.body.ResponseMessage;

public interface ClusterInvoker {

    ResponseMessage invoke(RequestMessage req);

}
