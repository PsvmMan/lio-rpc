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
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.gt.lio.remote.netty.NettyConstants.DEFAULT_THREADS;
import static com.gt.lio.remote.netty.NettyConstants.MAX_RECONNECT_TIMES;

public class NettyClient implements TransportClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    private static final EventLoopGroup workerGroup = new NioEventLoopGroup(DEFAULT_THREADS);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            workerGroup.shutdownGracefully();
        }));
    }

    private final ProtocolCodec codec;        // 编解码器（构造函数注入）
    private Bootstrap bootstrap;
    private Channel channel;

    private int retryCount = 0;

    private final Long writerIdleTime;

    private final String protocol;

    private final String host;

    private final Integer port;

    private boolean isClose = false;

    public NettyClient(ClientStartParam param) {
        this.writerIdleTime = param.getHeartbeatWriteTimeout() == null ? NettyConstants.WRITER_IDLE_TIME : param.getHeartbeatWriteTimeout();
        this.protocol = param.getProtocol();
        this.host = param.getHost();
        this.port = param.getPort();
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
                                .addLast(new NettyClientHandler(NettyClient.this));   // 业务处理器
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

    public void reConnect(){

        if(channel != null && channel.isActive()){
            return;
        }

        if (retryCount >= MAX_RECONNECT_TIMES) {
            if(logger.isInfoEnabled()){
                logger.info("The maximum number of reconnections has been reached, Netty Client  failed to connect to the server {}", host + ":" + port);
            }
            return;
        }

        ChannelFuture future = bootstrap.connect(host, port);
        if(logger.isInfoEnabled()){
            if(logger.isInfoEnabled()){
                logger.info("Netty Client {} is reconnecting to the server {}", channel.localAddress(), channel.remoteAddress());
            }
        }

        future.addListener((ChannelFutureListener) f -> {

            // 防止重连返回的时候，客户端已经关闭了
            if(isClose){
                f.channel().close();
                if(logger.isInfoEnabled()){
                    logger.info("Netty Client {} is closed from the server {}", channel.localAddress(), channel.remoteAddress());
                }
                return;
            }

            if (f.isSuccess()) {
                channel = f.channel();
                retryCount = 0;
                if(logger.isInfoEnabled()){
                    logger.info("Netty Client {} successfully connected to the server {}", channel.localAddress(), channel.remoteAddress());
                }
            } else {
                retryCount++;
                int delay = getBackoffDelay(retryCount);
                f.channel().eventLoop().schedule(this::reConnect, delay, java.util.concurrent.TimeUnit.SECONDS);
                if(logger.isInfoEnabled()){
                    logger.info("Netty Client {} failed to connect to the server {}, retrying in {} seconds", f.channel().localAddress(), f.channel().remoteAddress(), delay);
                }
            }
        });
    }

    /**
     * 获取指数退避延迟时间
     */
    private int getBackoffDelay(int attempt) {
        return (int) Math.pow(2, attempt); // 指数退避算法
    }

    @Override
    public boolean isAvailable() {
        return (channel != null && channel.isActive());
    }

    @Override
    public void send(ProtocolMessage data){
        channel.writeAndFlush(data);
    }

    @Override
    public boolean isClosed() {
        return isClose || retryCount >= MAX_RECONNECT_TIMES;
    }

    @Override
    public void close(){
        try {
            isClose = true;
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
