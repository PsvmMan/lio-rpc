package com.gt.lio.remote.netty.server;

import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.remote.ServerFactory;
import com.gt.lio.remote.param.ServerStartParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@SPIService("netty")
public class NettyServerFactory implements ServerFactory {
    private static final Logger logger = LoggerFactory.getLogger(NettyServerFactory.class);

    private static final ConcurrentHashMap<String, NettyServer> serverMap = new ConcurrentHashMap<>();

    public ReentrantLock lock = new ReentrantLock();

    @Override
    public void startServer(ServerStartParam param) {

        // ip和端口标识唯一服务
        String key = param.getHost() + ":" + param.getPort();

        // 如果服务已经存在，但是服务协议不一致，则抛出异常
        NettyServer nettyServer = serverMap.get(key);
        if(nettyServer != null){
            if(param.getProtocol().equals(nettyServer.getProtocol())){
                return;
            }else {
                throw new RuntimeException("NettyServerFactory startServer error, The address { "+ key +" } has been occupied by the " + nettyServer.getProtocol() + " protocol");
            }
        }

        lock.lock();
        try {
            nettyServer = serverMap.get(key);
            if(nettyServer != null){
                if(param.getProtocol().equals(nettyServer.getProtocol())){
                    return;
                }else {
                    throw new RuntimeException("NettyServerFactory startServer error, The address { "+ key +" } has been occupied by the " + nettyServer.getProtocol() + " protocol");
                }
            }
            nettyServer = new NettyServer(param);
            nettyServer.start();
            serverMap.put(key, nettyServer);
        }catch (Throwable e){
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    // 全局关闭钩子
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {

                for (NettyServer nettyServer : serverMap.values()) {
                    try {
                        nettyServer.stop();
                    } catch (Throwable e) {
                        if(logger.isErrorEnabled()){
                            logger.error("NettyServerFactory shutdown error", e);
                        }
                    }
                }

                serverMap.clear();

                if(logger.isInfoEnabled()){
                    logger.info("NettyServerFactory shutdown success");
                }
            }
        }));
    }
}
