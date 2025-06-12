package com.gt.lio.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.serialization.Serialization;
import com.gt.lio.serialization.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@SPIService(value ="kryo", code = 0x03)
public class KryoSerialization implements Serialization {

    // Kryo非线程安全，使用ThreadLocal
    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false); // 关闭类注册
        return kryo;
    });

    @Override
    public <T> byte[] serialize(T obj) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             Output output = new Output(bos)) {
            Kryo kryo = kryoThreadLocal.get();
            kryo.writeObject(output, obj);
            output.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new SerializationException("Kryo serialize failed", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             Input input = new Input(bis)) {
            Kryo kryo = kryoThreadLocal.get();
            return kryo.readObject(input, clazz);
        } catch (Exception e) {
            throw new SerializationException("Kryo deserialize failed", e);
        }
    }
}
