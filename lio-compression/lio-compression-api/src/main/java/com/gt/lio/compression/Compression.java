package com.gt.lio.compression;

import java.io.IOException;

public interface Compression {
    byte[] compress(byte[] data) throws IOException;
    byte[] decompress(byte[] compressedData) throws IOException;
}
