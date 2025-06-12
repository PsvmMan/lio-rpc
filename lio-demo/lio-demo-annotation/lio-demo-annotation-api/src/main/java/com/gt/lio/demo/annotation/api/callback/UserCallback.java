package com.gt.lio.demo.annotation.api.callback;

import com.gt.lio.common.callback.RpcCallback;
import com.gt.lio.demo.annotation.api.model.User;

public class UserCallback implements RpcCallback {

    @Override
    public void onSuccess(Object response) {
        if(response != null && response instanceof User){
            System.out.println("onSuccess: " + (User)response);
        }
    }

    @Override
    public void onFailure(Throwable cause) {
        Throwable err = cause.getCause();
        System.out.println("onFailure: " + err.getMessage());
    }
}
