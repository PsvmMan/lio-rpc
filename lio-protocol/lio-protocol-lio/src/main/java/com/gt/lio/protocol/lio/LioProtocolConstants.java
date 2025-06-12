package com.gt.lio.protocol.lio;

public class LioProtocolConstants {

    /**
     * 前8位表示字母L，ASCII编码中，大写字母'L'对应十六进制值 0x4C
     * 9~12 位表示主版本号
     * 13~16 位表示子版本号
     * 当前版本：Lio 1.0
     */
    public static final short MAGIC = 0x4C10;

    /**
     * 协议头总长度
     */
    public static final int HEADER_TOTAL_LEN = 16;

}
