package com.gt.lio.remote.netty.codec;

import com.gt.lio.protocol.ProtocolCodec;
import com.gt.lio.protocol.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageEncoder extends MessageToByteEncoder<ProtocolMessage> {

    private static final Logger logger = LoggerFactory.getLogger(MessageEncoder.class);

    private final ProtocolCodec codec;

    public MessageEncoder(ProtocolCodec codec) {
        this.codec = codec;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolMessage msg, ByteBuf out) {
        try {
            byte[] data = codec.encode(msg);
            out.writeBytes(data);
        } catch (Exception e) {
            logger.error("Encode failed", e);
        }
    }
}
