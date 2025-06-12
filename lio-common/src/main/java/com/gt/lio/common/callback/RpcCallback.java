package com.gt.lio.common.callback;

public interface RpcCallback {
    void onSuccess(Object response);
    void onFailure(Throwable cause);
}
