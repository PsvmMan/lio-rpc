package com.gt.lio.protocol.body;

import java.io.Serializable;

public class ResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应结果
     */
    private Object result;

    /**
     * 异常信息
     */
    private Throwable exception;

    public ResponseMessage() {
    }

    public ResponseMessage(Object result) {
        this.result = result;
    }

    public ResponseMessage(Throwable exception) {
        this.exception = exception;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }
}
