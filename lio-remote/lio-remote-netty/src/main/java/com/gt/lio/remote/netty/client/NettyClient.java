package com.gt.lio.remote.netty.client;

import com.gt.lio.common.spi.LioServiceLoader;
import com.gt.lio.protocol.ProtocolCodec;
import com.gt.lio.protocol.ProtocolMessage;
import com.gt.lio.remote.param.ClientStartParam;
import com.gt.lio.remote.TransportClient;
import com.gt.lio.remote.netty.codec.MessageDecoder;
import com.gt.lio.remote.netty.codec.MessageEncoder;
import com.gt.lio.remote.netty.NettyConstants;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class NettyClient implements TransportClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors() * 2;

    private static final EventLoopGroup workerGroup = new NioEventLoopGroup(DEFAULT_THREADS);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            workerGroup.shutdownGracefully();
        }));
    }

    private final ProtocolCodec codec;        // 编解码器（构造函数注入）
    private Bootstrap bootstrap;
    private Channel channel;

    private final Long writerIdleTime;

    private final String protocol;

    public NettyClient(ClientStartParam param) {
        this.writerIdleTime = param.getHeartbeatWriteTimeout() == null ? NettyConstants.WRITER_IDLE_TIME : param.getHeartbeatWriteTimeout();
        this.protocol = param.getProtocol();
        this.codec = LioServiceLoader.getServiceLoader(ProtocolCodec.class).getService(this.protocol);
        initBootstrap();
    }

    private void initBootstrap() {

        bootstrap = new Bootstrap()
                .group(workerGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new IdleStateHandler(0, writerIdleTime, 0, TimeUnit.MILLISECONDS)) // 20秒写空闲检测
                                .addLast(new MessageEncoder(codec))   // 编码器
                                .addLast(new MessageDecoder(codec))   // 解码器
                                .addLast(new NettyClientHandler());   // 业务处理器
                    }
                });
    }

    @Override
    public void connect(String host, int port){
        try {
            channel = bootstrap.connect(host, port).sync().channel();
            if(logger.isInfoEnabled()){
                logger.info("Netty Client {} successfully connected to the server {}", channel.localAddress(), channel.remoteAddress());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(ProtocolMessage data){
        channel.writeAndFlush(data);
    }

    @Override
    public void close(){
        try {
            if(channel != null){

                channel.close();

                if(logger.isInfoEnabled()){
                    logger.info("Netty Client {} is disconnected from the server {}", channel.localAddress(), channel.remoteAddress());
                }

            }
        }catch (Throwable e){
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public String getLocalAddress() {
        return channel.localAddress().toString();
    }

    @Override
    public String getRemoteAddress() {
        return channel.remoteAddress().toString();
    }
}
