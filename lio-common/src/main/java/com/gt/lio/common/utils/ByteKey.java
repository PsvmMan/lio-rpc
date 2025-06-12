package com.gt.lio.common.utils;

/**
 * 在Map中想要使用byte作为key，但是Byte没有重写equals和hashCode方法，所以不能直接使用Byte作为key
 * 所以使用这个类
 */
public class ByteKey {

    private final byte value;

    public ByteKey(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ByteKey byteKey = (ByteKey) o;
        return value == byteKey.value;
    }

    @Override
    public int hashCode() {
        return Byte.hashCode(value);
    }

}
