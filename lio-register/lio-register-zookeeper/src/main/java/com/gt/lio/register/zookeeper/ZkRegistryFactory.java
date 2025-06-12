package com.gt.lio.register.zookeeper;

import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.config.RegistryConfig;
import com.gt.lio.register.RegistryFactory;
import com.gt.lio.register.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@SPIService("zookeeper")
public class ZkRegistryFactory implements RegistryFactory {

    private static final Logger logger = LoggerFactory.getLogger(ZkRegistryFactory.class);

    private static final Map<String, RegistryService> CLIENT_CACHE = new ConcurrentHashMap<>();

    private static final ReentrantLock LOCK = new ReentrantLock();

    @Override
    public RegistryService getRegistry(RegistryConfig config) {

        // zookeeper客户端的唯一标识：ip:port
        String key = config.getAddress();

        // 尝试获取缓存中的客户端
        RegistryService registryService = CLIENT_CACHE.get(key);

        // 如果客户端已经存在且连接正常，则直接返回客户端
        if (registryService != null && ((ZkRegistryService) registryService).isConnected()) {
            return registryService;
        }

        // 如果客户端不存在或者连接断开，则重新创建客户端
        LOCK.lock();
        try {

            // 尝试获取缓存中的客户端
            registryService = CLIENT_CACHE.get(key);

            // 如果客户端已经存在但连接断开，则移除客户端并销毁客户端
            if (registryService != null && !((ZkRegistryService) registryService).isConnected()) {
                RegistryService remove = CLIENT_CACHE.remove(key);
                remove.destroy();
                registryService = null;
            }

            // 如果客户端不存在，则创建客户端
            if (registryService == null) {
                registryService = new ZkRegistryService(config);
                if(registryService == null){
                    throw new IllegalArgumentException(key + " is not a valid zookeeper address, create failed！");
                }
                CLIENT_CACHE.put(key, registryService);
            }
        }catch (Exception e){
            logger.error("Failed to create ZooKeeper client for {}", key, e);
            throw new RuntimeException(e);
        }finally {
            LOCK.unlock();
        }

        return registryService;
    }

    /**
     * 销毁所有缓存的ZooKeeper客户端
     * 此方法通过锁定确保线程安全，在销毁过程中，会遍历客户端缓存，并尝试销毁每个客户端
     * 如果销毁成功，会记录相应的日志信息；如果销毁失败，则记录错误信息
     */

    public static  void destroyAll() {
        // 加锁，确保线程安全
        LOCK.lock();
        try {
            // 遍历客户端缓存，对每个客户端尝试进行销毁操作
            CLIENT_CACHE.forEach((key, registry) -> {
                try {
                    // 尝试销毁客户端
                    registry.destroy();
                    // 记录销毁成功的日志信息
                    logger.info("Closed ZooKeeper client: {}", key);
                } catch (Exception e) {
                    // 如果销毁失败，记录错误日志信息
                    logger.error("Failed to close ZooKeeper client: {}", key, e);
                }
            });
            // 清空客户端缓存
            CLIENT_CACHE.clear();
        } finally {
            // 无论如何，最终都要释放锁
            LOCK.unlock();
        }
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // 在JVM关闭时，调用销毁所有ZooKeeper客户端的方法
            destroyAll();
        }));
    }

}
