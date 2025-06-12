package com.gt.lio.config.spring.utils;

import java.lang.reflect.Method;
import java.util.Map;

public class LioPropertyMapper {

    public static <T> T map(Map<String, Object> properties, Class<T> clazz) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value == null) continue;

                // 将 key 转换为首字母大写的 setter 方法名
                String setterName = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
                Method setterMethod = findMatchingSetter(clazz, setterName, value);

                if (setterMethod != null) {
                    setterMethod.invoke(instance, convertValue(setterMethod.getParameterTypes()[0], value));
                }
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping properties to class: " + clazz.getName(), e);
        }
    }

    private static Method findMatchingSetter(Class<?> clazz, String methodName, Object value) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (paramType.isAssignableFrom(value.getClass()) || canConvert(value.getClass(), paramType)) {
                    return method;
                }
            }
        }
        return null;
    }

    private static boolean canConvert(Class<?> fromType, Class<?> toType) {
        // 基本类型及其包装类之间的转换
        if (fromType.equals(String.class)) {
            return toType.equals(Integer.class) || toType.equals(int.class) ||
                    toType.equals(Long.class) || toType.equals(long.class) ||
                    toType.equals(Boolean.class) || toType.equals(boolean.class) ||
                    toType.equals(Double.class) || toType.equals(double.class) ||
                    toType.equals(Float.class) || toType.equals(float.class);
        }
        return false;
    }

    private static Object convertValue(Class<?> targetType, Object value) {
        if (targetType.isInstance(value)) {
            return value;
        } else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            return Integer.parseInt(value.toString());
        } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
            return Long.parseLong(value.toString());
        } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
            return Boolean.parseBoolean(value.toString());
        } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
            return Double.parseDouble(value.toString());
        } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
            return Float.parseFloat(value.toString());
        }
        // 如果无法转换，则返回原值或抛出异常，视具体需求而定
        throw new IllegalArgumentException("Cannot convert value of type " + value.getClass() + " to target type " + targetType);
    }
}
