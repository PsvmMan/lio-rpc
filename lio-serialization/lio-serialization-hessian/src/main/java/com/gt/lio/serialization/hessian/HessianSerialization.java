package com.gt.lio.serialization.hessian;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.serialization.Serialization;
import com.gt.lio.serialization.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@SPIService(value = "hessian", code = 0x02)
public class HessianSerialization implements Serialization {

    @Override
    public <T> byte[] serialize(T obj) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Hessian2Output output = new Hessian2Output(bos);
            output.writeObject(obj);
            output.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Hessian serialize failed", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            Hessian2Input input = new Hessian2Input(bis);
            return (T) input.readObject(clazz);
        } catch (IOException e) {
            throw new SerializationException("Hessian deserialize failed", e);
        }
    }
}
