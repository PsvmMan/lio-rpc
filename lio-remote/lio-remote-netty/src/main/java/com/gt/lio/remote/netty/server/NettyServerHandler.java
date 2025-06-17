package com.gt.lio.remote.netty.server;

import com.gt.lio.common.spi.LioServiceLoader;
import com.gt.lio.compression.Compression;
import com.gt.lio.config.model.LioServiceMethodMetadata;
import com.gt.lio.common.invoker.RpcInvoker;
import com.gt.lio.common.invoker.RpcInvokerProvider;
import com.gt.lio.protocol.ProtocolConstants;
import com.gt.lio.protocol.ProtocolMessage;
import com.gt.lio.protocol.body.RequestMessage;
import com.gt.lio.protocol.body.ResponseMessage;
import com.gt.lio.protocol.header.ProtocolHeader;
import com.gt.lio.common.threadpool.ThreadPoolProvider;
import com.gt.lio.common.threadpool.ThreadPoolFactory;
import com.gt.lio.serialization.Serialization;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;


public class NettyServerHandler extends SimpleChannelInboundHandler<ProtocolMessage> {

    private static final Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

    /**
     * 客户端连接到服务器时触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(logger.isInfoEnabled()){
            logger.info("Client connected: " + ctx.channel().remoteAddress());
        }
        super.channelActive(ctx);
    }

    /**
     * 客户端断开连接时触发
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(logger.isInfoEnabled()){
            logger.info("Client disconnected: " + ctx.channel().remoteAddress());
        }
        super.channelInactive(ctx);
    }

    /**
     * 处理客户端发来的 ProtocolMessage 消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtocolMessage msg) {
        try {

            // 获取协议头
            ProtocolHeader header = msg.getHeader();

            // 获取请求体
            byte[] msgBody = msg.getBody();

            // 心跳消息直接返回
            if(header == null || header.getMsgType() == ProtocolConstants.HEARTBEAT_MESSAGE ||
                    msgBody == null || msgBody.length == 0){
                return;
            }

            // 获取协议头中的线程池编码，转为线程名称
            String threadPoolName = LioServiceLoader.getServiceLoader(ThreadPoolFactory.class).getServiceNameByCode(header.getThreadPoolName());

            // 序列化实现类
            LioServiceLoader<Serialization> serializationServiceLoader = LioServiceLoader.getServiceLoader(Serialization.class);
            Serialization serialization = serializationServiceLoader.getService(serializationServiceLoader.getServiceNameByCode(header.getSerializationType()));

            // 业务线程池
            ThreadPoolProvider.instance.getThreadPool(threadPoolName).execute(() -> {

                try {

                    byte[] body = msgBody;

                    LioServiceLoader<Compression> compressionServiceLoader = LioServiceLoader.getServiceLoader(Compression.class);

                    // 如果需要解压消息
                    if(header.isCompressed()){
                        Compression compression = compressionServiceLoader.getService(compressionServiceLoader.getServiceNameByCode(header.getCompressionType()));
                        body = compression.decompress(body);
                    }

                    // 反序列化消息
                    RequestMessage requestMessage = serialization.deserialize(body, RequestMessage.class);

                    // 获取执行器
                    RpcInvoker rpcInvoker = RpcInvokerProvider.instance.getInvoker(requestMessage.getServiceName());
                    if(rpcInvoker == null){
                        throw new RuntimeException("No invoker for service [" + requestMessage.getServiceName() + "]");
                    }

                    // 执行业务
                    Object result = rpcInvoker.invoke(requestMessage.getMethodName(), requestMessage.getArgs(), requestMessage.getParamTypes());

                    // 如果需要返回结果
                    if(header.isRespond()){

                        //响应消息
                        ResponseMessage responseMessage = new ResponseMessage();
                        responseMessage.setResult(result);

                        // 序列化响应消息
                        byte[] responseBody = serialization.serialize(responseMessage);

                        // 压缩响应消息
                        Map<String, LioServiceMethodMetadata> methods = (Map<String, LioServiceMethodMetadata>)rpcInvoker.getMetadata().get("methods");
                        LioServiceMethodMetadata methodMetadata = methods.getOrDefault(requestMessage.getMethodKey(),methods.get(LioServiceMethodMetadata.DEFAULT));
                        if(methodMetadata != null && methodMetadata.isCompressed()){
                            String compressionType = methodMetadata.getCompressionType();
                            responseBody = compressionServiceLoader.getService(compressionType).compress(responseBody);
                            // 告诉客户端，消息解压的算法
                            msg.getHeader().setCompressionType(compressionServiceLoader.getCodeByServiceName(compressionType));
                        }else {
                            // 告诉客户端，消息不压缩
                            msg.getHeader().setCompressionType(ProtocolConstants.NO_COMPRESSED);
                        }

                        msg.setBody(responseBody);

                        // 发送消息
                        ctx.writeAndFlush(msg);
                    }
                }catch (Throwable e){
                    if(header.isRespond()){

                        // 错误响应信息
                        ResponseMessage responseMessage = new ResponseMessage(e);

                        // 序列化消息
                        byte[] responseBody = serialization.serialize(responseMessage);

                        msg.setBody(responseBody);

                        //错误信息没必要压缩
                        msg.getHeader().setCompressionType(ProtocolConstants.NO_COMPRESSED);

                        // 发送消息
                        ctx.writeAndFlush(msg);
                    }
                }

            });

        } catch (Throwable e) {
            if(logger.isErrorEnabled()){
                logger.error("Error occurred while processing requestMessage from client [" + ctx.channel().remoteAddress() + "]: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 出现异常时触发
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if(logger.isErrorEnabled()){
            logger.error("Exception caught from client [" + ctx.channel().remoteAddress() + "]: " + cause.getMessage(), cause);
        }
        ctx.close(); // 关闭连接
    }


    /**
     * 读空闲时触发
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            if(logger.isInfoEnabled()){
                logger.info("Client [ "+ctx.channel().remoteAddress() +" ] " + " is write timeout, close it");
            }
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
