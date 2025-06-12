package com.gt.lio.compression.gzip;

import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.compression.Compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@SPIService(value = "gzip", code = 0x02)
public class GzipCompression implements Compression {

    private static final ThreadLocal<byte[]> BUFFER_HOLDER = ThreadLocal.withInitial(() -> new byte[8192]);

    @Override
    public byte[] compress(byte[] data) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOS = new GZIPOutputStream(bos)) {
            gzipOS.write(data);
            gzipOS.finish();
            return bos.toByteArray();
        }
    }

    @Override
    public byte[] decompress(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipIS = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length * 2)) {
            byte[] buffer = BUFFER_HOLDER.get(); // ThreadLocal 缓冲区
            int len;
            while ((len = gzipIS.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }
}
