package com.gt.lio.config.model;

import com.gt.lio.common.callback.EmptyCallback;
import com.gt.lio.common.callback.RpcCallback;
import com.gt.lio.config.annotation.LioReferenceMethod;

public class LioReferenceMethodMetadata {

    public static final String DEFAULT = "*";

    private long timeout;

    private int retries;

    private int parallelNumber;

    private boolean isRespond;

    private boolean isCompressed;

    private String compressionType;

    private String cluster;

    private String loadBalance;

    private boolean isAsync;

    private RpcCallback callback;

    public LioReferenceMethodMetadata(LioReferenceMethod lioMethod) {
        if(lioMethod.timeout() < 0){
            throw new IllegalArgumentException("timeout must be greater than 0");
        }
        if(lioMethod.retries() < 0){
            throw new IllegalArgumentException("retries must be greater than 0");
        }
        if(lioMethod.parallelNumber() < 0){
            throw new IllegalArgumentException("parallelNumber must be greater than 0");
        }
        if(lioMethod.cluster().isEmpty()){
            throw new IllegalArgumentException("cluster must not be empty");
        }
        if(lioMethod.loadBalance().isEmpty()){
            throw new IllegalArgumentException("loadBalance must not be empty");
        }
        if(lioMethod.isCompressed()){
            if(lioMethod.compressionType().isEmpty()){
                throw new IllegalArgumentException("compressionType must not be empty");
            }
        }
        this.timeout = lioMethod.timeout();
        this.retries = lioMethod.retries();
        this.parallelNumber = lioMethod.parallelNumber();
        this.isRespond = lioMethod.isRespond();
        this.isCompressed = lioMethod.isCompressed();
        this.compressionType = lioMethod.compressionType();
        this.cluster = lioMethod.cluster();
        this.loadBalance = lioMethod.loadBalance();
        this.isAsync = lioMethod.isAsync();

        try {
            this.callback = lioMethod.callback().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to create callback instance of class: " + lioMethod.callback().getName(), e);
        }
    }


    public boolean isAsync() {
        return isAsync;
    }

    public void setAsync(boolean async) {
        isAsync = async;
    }

    public RpcCallback getCallback() {
        return callback;
    }

    public void setCallback(RpcCallback callback) {
        this.callback = callback;
    }

    public LioReferenceMethodMetadata() {
    }

    public int getParallelNumber() {
        return parallelNumber;
    }

    public void setParallelNumber(int parallelNumber) {
        this.parallelNumber = parallelNumber;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public boolean isRespond() {
        return isRespond;
    }

    public void setRespond(boolean respond) {
        isRespond = respond;
    }

    public boolean isCompressed() {
        return isCompressed;
    }

    public void setCompressed(boolean compressed) {
        isCompressed = compressed;
    }

    public String getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getLoadBalance() {
        return loadBalance;
    }

    public void setLoadBalance(String loadBalance) {
        this.loadBalance = loadBalance;
    }
}
