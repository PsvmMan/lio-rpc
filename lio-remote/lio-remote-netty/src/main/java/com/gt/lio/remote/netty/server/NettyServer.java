package com.gt.lio.remote.netty.server;

import com.gt.lio.common.spi.LioServiceLoader;
import com.gt.lio.protocol.ProtocolCodec;
import com.gt.lio.remote.param.ServerStartParam;
import com.gt.lio.remote.TransportServer;
import com.gt.lio.remote.netty.codec.MessageDecoder;
import com.gt.lio.remote.netty.codec.MessageEncoder;
import com.gt.lio.remote.netty.NettyConstants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static com.gt.lio.remote.netty.NettyConstants.DEFAULT_THREADS;

public class NettyServer implements TransportServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private final ProtocolCodec codec;

    private final Long sessionTimeout;

    private final String host;

    private final Integer port;

    private final String protocol;

    private ServerBootstrap serverBootstrap;

    private Channel channel;

    NioEventLoopGroup boss;

    NioEventLoopGroup works;


    public NettyServer(ServerStartParam param) {
        this.sessionTimeout = param.getHeartbeatReadTimeout() == null ? NettyConstants.READ_IDLE_TIME : param.getHeartbeatReadTimeout();
        this.host = param.getHost();
        this.port = param.getPort();
        this.protocol = param.getProtocol();
        this.codec = LioServiceLoader.getServiceLoader(ProtocolCodec.class).getService(this.protocol);
    }

    @Override
    public void start(){

        boss = new NioEventLoopGroup(1);
        works = new NioEventLoopGroup(DEFAULT_THREADS);

        serverBootstrap = new ServerBootstrap()
                .group(boss, works)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // 动态添加编解码器和心跳检测
                        ch.pipeline()
                                .addLast(new IdleStateHandler(sessionTimeout, 0, 0, TimeUnit.MILLISECONDS)) // 30秒读空闲检测
                                .addLast(new MessageEncoder(codec))   // 编码器
                                .addLast(new MessageDecoder(codec))   // 解码器
                                .addLast(new NettyServerHandler());   // 业务处理器
                    }
                });

        ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(host, port));
        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();

        if(logger.isInfoEnabled()){
            logger.info("Netty Server {} started.", getKey());
        }

    }

    @Override
    public void stop(){

        try {

            if (boss != null) {
                boss.shutdownGracefully();
            }

            if (works != null) {
                works.shutdownGracefully();
            }

            if (channel != null) {
                channel.close();
            }

            if(logger.isInfoEnabled()){
                logger.info("Netty Server {} closed.", getKey());
            }

        } catch (Throwable e) {
            if(logger.isErrorEnabled()){
                logger.error(e.getMessage(), e);
            }
        }

    }

    public String getKey(){
        return protocol + ":" + host + ":" + port;
    }

    public String getProtocol(){
        return protocol;
    }
}
