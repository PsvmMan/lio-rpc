package com.gt.lio.remote.netty.client;

import com.gt.lio.common.spi.LioServiceLoader;
import com.gt.lio.common.utils.ResponseHolder;
import com.gt.lio.compression.Compression;
import com.gt.lio.protocol.ProtocolConstants;
import com.gt.lio.protocol.ProtocolMessage;
import com.gt.lio.protocol.body.ResponseMessage;
import com.gt.lio.protocol.header.ProtocolHeader;
import com.gt.lio.serialization.Serialization;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.gt.lio.protocol.ProtocolConstants.HEARTBEAT_DATA;
import static com.gt.lio.remote.RemoteConstants.DEFAULT_DELIMITER;

public class NettyClientHandler extends SimpleChannelInboundHandler<ProtocolMessage> {

    private static final Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);

    /**
     * 客户端连接到服务器时触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client connected: [ remoteAddress: " + ctx.channel().remoteAddress() + " , localAddress: " + ctx.channel().localAddress() + "]");
        super.channelActive(ctx);
    }

    /**
     * 客户端断开连接时触发
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client disconnected: [ remoteAddress: " + ctx.channel().remoteAddress() + " , localAddress: " + ctx.channel().localAddress() + "]");
        super.channelInactive(ctx);
    }

    /**
     * 处理客户端发来的 ProtocolMessage 消息
     *
     * @param ctx 上下文对象，包含 Channel、EventLoop 等信息
     * @param msg 解码后的 ProtocolMessage 对象
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtocolMessage msg) throws Exception {

        // 客户端业务线程池处理
        ResponseProcessor.submitRequest(() -> {

            // 本地地址
            String localAddress = ctx.channel().localAddress().toString();

            // 远程地址
            String remoteAddress = ctx.channel().remoteAddress().toString();

            // 请求 ID
            String requestId = String.valueOf(msg.getHeader().getRequestId());

            // key
            String key = remoteAddress + DEFAULT_DELIMITER + localAddress + DEFAULT_DELIMITER + requestId;

            try {
                // 获取协议头
                ProtocolHeader header = msg.getHeader();

                // 获取请求体
                byte[] body = msg.getBody();

                if(header.getMsgType() == ProtocolConstants.HEARTBEAT_MESSAGE || body == null || body.length == 0){
                    return;
                }

                // 解压
                if(header.isCompressed()){
                    LioServiceLoader<Compression> serviceLoader = LioServiceLoader.getServiceLoader(Compression.class);
                    Compression compression = serviceLoader.getService(serviceLoader.getServiceNameByCode(header.getCompressionType()));
                    body = compression.decompress(body);
                }

                // 反序列化
                LioServiceLoader<Serialization> serviceLoader = LioServiceLoader.getServiceLoader(Serialization.class);
                Serialization serialization = serviceLoader.getService(serviceLoader.getServiceNameByCode(header.getSerializationType()));
                ResponseMessage responseMessage = serialization.deserialize(body, ResponseMessage.class);

                // 尝试处理结果
                if(!ResponseHolder.complete(key, responseMessage)){
                    // 尝试处理并发调用结果，执行成功或者全部失败，才返回结果
                    if(responseMessage.getException() == null || ResponseHolder.canComplete(requestId)){
                        ResponseHolder.complete(requestId, responseMessage);
                    }
                }
            } catch (Exception e) {
                ResponseMessage responseMessage = new ResponseMessage(e);
                if(!ResponseHolder.complete(key, responseMessage) && ResponseHolder.canComplete(requestId)){
                    // 处理并发调用结果
                    ResponseHolder.complete(requestId, responseMessage);
                }
            }
        });

    }

    /**
     * 出现异常时触发
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception caught from client [ remoteAddress: " + ctx.channel().remoteAddress() + " , localAddress: " + ctx.channel().localAddress() +"]: " + cause.getMessage(), cause);
        ctx.close(); // 关闭连接
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            // 写空闲，发送心跳包
            ctx.writeAndFlush(HEARTBEAT_DATA);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

}
