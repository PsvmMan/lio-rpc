package com.gt.lio.config.model;

import com.gt.lio.config.annotation.LioServiceMethod;

public class LioServiceMethodMetadata {

    public static final String DEFAULT = "*";

    private boolean isCompressed;

    private String compressionType;


    public LioServiceMethodMetadata(LioServiceMethod lioMethod) {
        if(lioMethod.isCompressed()){
            if(lioMethod.compressionType().isEmpty()){
                throw new IllegalArgumentException("compressionType must not be empty");
            }
        }
        this.isCompressed = lioMethod.isCompressed();
        this.compressionType = lioMethod.compressionType();
    }

    public LioServiceMethodMetadata() {
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


}
