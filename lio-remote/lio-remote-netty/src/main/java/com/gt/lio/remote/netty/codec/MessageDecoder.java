package com.gt.lio.remote.netty.codec;

import com.gt.lio.protocol.ProtocolCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MessageDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(MessageDecoder.class);

    private final ProtocolCodec codec;

    public MessageDecoder(ProtocolCodec codec) {
        this.codec = codec;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {

        // 标记原始读指针位置
        final int readerIndexBefore = in.readerIndex();

        try {

            // 将数据转为字节数组，此时读指针已经移动到writerIndex
            byte[] data = new byte[in.readableBytes()];
            in.readBytes(data);

            // 调用协议解码（注意：codec.decode需要实现返回已处理字节数）
            int processedBytes = codec.decode(data, out);

            in.readerIndex(readerIndexBefore + processedBytes);

        } catch (Exception e) {
            in.readerIndex(readerIndexBefore);
            out.clear();
            logger.error("Decode failed", e);
        }
    }
}
