package com.gt.lio.remote;

import com.gt.lio.remote.param.ClientStartParam;

public interface ClientFactory {
    TransportClient createClient(ClientStartParam param);
}
