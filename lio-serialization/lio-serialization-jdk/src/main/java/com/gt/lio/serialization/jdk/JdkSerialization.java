package com.gt.lio.serialization.jdk;

import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.serialization.Serialization;
import com.gt.lio.serialization.SerializationException;


import java.io.*;

@SPIService(value = "jdk", code = 0x01)
public class JdkSerialization implements Serialization {

    @Override
    public <T> byte[] serialize(T obj) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("JDK serialize failed", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("JDK deserialize failed", e);
        }
    }
}
