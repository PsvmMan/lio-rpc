package com.gt.lio.compression.zstd;

import com.github.luben.zstd.Zstd;
import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.compression.Compression;

import java.io.IOException;

@SPIService(value = "zstd", code = 0x01)
public class ZstdCompression implements Compression {

    @Override
    public byte[] compress(byte[] data) throws IOException {
        try {
            return Zstd.compress(data);
        } catch (Exception e) {
            throw new IOException("Error during compression: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] decompress(byte[] compressedData) throws IOException {
        try {
            long decompressedSize = Zstd.decompressedSize(compressedData);
            if (decompressedSize == -1) {
                throw new IOException("Unable to determine decompressed size.");
            }
            return Zstd.decompress(compressedData, (int) decompressedSize);
        } catch (Exception e) {
            throw new IOException("Error during decompression: " + e.getMessage(), e);
        }
    }
}
