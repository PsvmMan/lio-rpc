package com.gt.lio.common.callback;

public class EmptyCallback implements RpcCallback {
    @Override public void onSuccess(Object response) {}
    @Override public void onFailure(Throwable cause) {}
}
