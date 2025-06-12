package com.gt.lio.serialization;

public interface Serialization {

    /**
     * 序列化对象为字节数组
     */
    <T> byte[] serialize(T obj) throws SerializationException;

    /**
     * 反序列化字节数组为对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz) throws SerializationException;

}
