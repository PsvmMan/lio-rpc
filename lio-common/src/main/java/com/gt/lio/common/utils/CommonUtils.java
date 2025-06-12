package com.gt.lio.common.utils;

public class CommonUtils {

    public static String generateMethodKey(String methodName, Class<?>[] paramTypes) {
        StringBuilder sb = new StringBuilder(methodName);
        for (Class<?> paramType : paramTypes) {
            sb.append("#").append(paramType.getName());
        }
        return sb.toString();
    }

}
