package com.gt.lio.register.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gt.lio.config.RegistryConfig;
import com.gt.lio.register.NotifyListener;
import com.gt.lio.register.RegistryService;
import com.gt.lio.register.ServiceInstance;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

public class ZkRegistryService implements RegistryService {

    // 默认连接超时时间（毫秒）
    public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 5000;

    // 默认会话超时时间（毫秒）
    public static final int DEFAULT_SESSION_TIMEOUT_MS = 30000;

    // 默认重试间隔（毫秒）
    public static final int DEFAULT_RETRY_INTERVAL_MS = 1000;

    // 默认最大重试次数
    public static final int DEFAULT_MAX_RETRIES = 3;

    private static final Logger logger = LoggerFactory.getLogger(ZkRegistryService.class);

    private static final String BASE_PATH = "/lio";

    private final CuratorFramework client;
    private final RegistryConfig config;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, ServiceInstance> registeredInstances = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, PathChildrenCache> serviceCaches = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Set<NotifyListener>> serviceListenersMap = new ConcurrentHashMap<>();

    private volatile boolean isConnected = false;

    public ZkRegistryService(RegistryConfig config) {

        this.config = config;

        try {
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                    // 连接地址
                    .connectString(config.getAddress())
                    // 重试策略
                    .retryPolicy(new ExponentialBackoffRetry(
                            config.getRetryIntervalMs() == null ? DEFAULT_RETRY_INTERVAL_MS : config.getRetryIntervalMs(),
                            config.getMaxRetries() == null ? DEFAULT_MAX_RETRIES : config.getMaxRetries()))
                    // 连接超时
                    .connectionTimeoutMs(config.getConnectionTimeoutMs() == null ? DEFAULT_CONNECTION_TIMEOUT_MS : config.getConnectionTimeoutMs())
                    // 会话超时
                    .sessionTimeoutMs(config.getSessionTimeoutMs() == null ? DEFAULT_SESSION_TIMEOUT_MS : config.getSessionTimeoutMs());

            // 添加用户名密码认证
            String authority = config.getAuthority();
            if (authority != null && authority.length() > 0) {
                builder = builder.authorization("digest", authority.getBytes());
            }

            client = builder.build();

            CountDownLatch connectedLatch = new CountDownLatch(1);

            // 添加连接状态监听器
            client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
                @Override
                public void stateChanged(CuratorFramework client, ConnectionState state) {
                    // 1. 连接完全丢失（会话失效）
                    if (state == ConnectionState.LOST) {
                        handleConnectionLost();
                    }
                    // 2. 连接成功建立
                    else if (state == ConnectionState.CONNECTED) {
                        handleConnected();
                    }
                    // 3. 断开后重新连接成功（会话可能恢复）
                    else if (state == ConnectionState.RECONNECTED) {
                        handleReconnected();
                    }
                    // 4. 连接中断
                    else if (state == ConnectionState.SUSPENDED) {
                        handleConnectionLost();
                    }

                    connectedLatch.countDown();

                }
            });

            // 启动客户端
            client.start();

            connectedLatch.await();

        }catch (Exception e){
            throw new RuntimeException("Failed to connect to Zookeeper server", e);
        }

    }

    private void handleConnectionLost() {
        logger.error("ZooKeeper connection LOST! Session expired" + ": " + config.getAddress());
        isConnected = false;
    }

    private void handleConnected() {
        logger.info("Successfully connected to ZooKeeper" + ": " + config.getAddress());
        isConnected = true;
    }

    private void handleReconnected() {
        logger.warn("Reconnected to ZooKeeper, recovering" + ": " + config.getAddress());
        isConnected = true;
        reRegisterServices();
        reSubscribeServices();
    }

    private void reRegisterServices() {
        registeredInstances.values().forEach(instance -> {
            try {
                register(instance);
                logger.info("Re-registered service: {}", instance.generateInstanceKey());
            } catch (Exception e) {
                logger.error("Failed to re-register service: {}", instance.generateInstanceKey(), e);
            }
        });
    }

    private void reSubscribeServices() {
        // 遍历所有已注册的服务名
        for (String serviceName : serviceListenersMap.keySet()) {
            // 获取该服务的所有监听器
            Set<NotifyListener> listeners = serviceListenersMap.get(serviceName);

            if (listeners != null && !listeners.isEmpty()) {
                // 遍历监听器集合进行重新订阅
                for (NotifyListener listener : listeners) {
                    try {
                        unsubscribe(serviceName, listener);
                        subscribe(serviceName, listener);
                        logger.info("Re-subscribed: {}", serviceName);
                    } catch (Exception e) {
                        logger.error("Failed to re-subscribe: {}", serviceName, e);
                    }
                }
            }
        }
    }

    @Override
    public void register(ServiceInstance instance) {
        try {

            ServiceInstance previous = registeredInstances.putIfAbsent(instance.generateInstanceKey(), instance);
            if (previous != null) {
                logger.warn("Service already registered: {}", instance);
                return;
            }

            // 1. 构建持久化服务路径（如：/lio/com.gt.lio.demo.annotation.provider.inf.DemoService:dev:1.0）
            String servicePath  = buildInstancePath(instance);
            createPersistentPath(servicePath);

            // 2. 构建临时实例节点路径（如：/lio/com.gt...DemoService:1.0:dev/lio:10.102.1.4:20888）
            String instancePath = ZKPaths.makePath(servicePath, instance.generateInstanceName());

            // 序列化服务实例信息
            byte[] data = serializeInstance(instance);

            // 创建临时节点来注册服务实例
            client.create()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(instancePath, data);

            logger.info("Registered service instance: {}", instancePath);
        } catch (Exception e) {
            // 如果注册失败，抛出运行时异常
            throw new RuntimeException("Registration failed: " + instance.generateInstanceKey(), e);
        }
    }

    @Override
    public void unregister(ServiceInstance instance) {
        try {
            String servicePath  = buildInstancePath(instance);
            String instancePath = ZKPaths.makePath(servicePath , instance.generateInstanceName());

            // 使用客户端删除路径，guaranteed()确保路径存在时才删除
            client.delete().guaranteed().forPath(instancePath);

            registeredInstances.remove(instance.generateInstanceKey());

            logger.info("Unregistered service instance: {}", instancePath);
        } catch (Exception e) {
            // 如果出现异常，抛出运行时异常，表明注销失败
            throw new RuntimeException("Unregistration failed: " + instance.generateInstanceKey(), e);
        }
    }

    @Override
    public void subscribe(String serviceName, NotifyListener listener) {

        // 获取或创建监听器集合（线程安全）
        serviceListenersMap.computeIfAbsent(serviceName, k -> ConcurrentHashMap.newKeySet()).add(listener);

        // 获取或创建 PathChildrenCache（线程安全）
        serviceCaches.computeIfAbsent(serviceName, name -> {

            PathChildrenCache newCache = new PathChildrenCache(client, buildServicePath(name), true);

            // 添加监听器，当子节点变化时通知所有监听者
            newCache.getListenable().addListener((client, event) -> {
                // 当连接重新连接时刷新缓存
                if (event.getType() == PathChildrenCacheEvent.Type.CONNECTION_RECONNECTED) {
                    refreshCache(newCache);
                }

                // 获取当前服务的所有实例
                List<ServiceInstance> instances = getInstances(name);

                // 通知所有订阅该服务的监听器
                Set<NotifyListener> listeners = serviceListenersMap.get(name);
                if (listeners != null) {
                    for (NotifyListener l : listeners) {
                        l.notify(instances, config.getAddress());
                    }
                }
            });

            try {
                newCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
                logger.info("Subscribed to service: {}", name);
            } catch (Exception e) {
                logger.error("Failed to start cache for service: {}", name, e);
                throw new RuntimeException("Subscribe failed: " + name, e);
            }

            return newCache;
        });
    }


    @Override
    public void unsubscribe(String serviceName, NotifyListener listener) {
        // 获取监听器集合
        Set<NotifyListener> listeners = serviceListenersMap.get(serviceName);

        if (listeners != null) {
            // 移除指定的监听器
            listeners.remove(listener);

            // 如果监听器集合为空了，说明没人再关注这个服务了
            if (listeners.isEmpty()) {
                // 移除并关闭 PathChildrenCache
                PathChildrenCache cache = serviceCaches.remove(serviceName);
                if (cache != null) {
                    try {
                        cache.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to close cache for service: " + serviceName, e);
                    }
                }
            }
        }
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) {
        try {

            String servicePath = buildServicePath(serviceName);
            List<ServiceInstance> instances = new ArrayList<>();

            // 检查服务路径是否存在
            if (client.checkExists().forPath(servicePath) == null) {
                return instances;
            }

            // 获取服务路径下的所有子节点
            List<String> childNodes = client.getChildren().forPath(servicePath);

            for (String child : childNodes) {
                try {
                    String instancePath = ZKPaths.makePath(servicePath, child);
                    byte[] data = client.getData().forPath(instancePath);
                    ServiceInstance instance = deserializeInstance(data);
                    instances.add(instance);
                } catch (Exception e) {
                    logger.error("Failed to deserialize instance: {}", child, e);
                }
            }

            return instances;
        } catch (Exception e) {
            throw new RuntimeException("Get instances failed: " + serviceName, e);
        }
    }

    private void createPersistentPath(String path) throws Exception {
        if (client.checkExists().forPath(path) == null) {
            client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(path);
        }
    }

    private String buildInstancePath(ServiceInstance instance) {
        return ZKPaths.makePath(BASE_PATH, instance.getServiceName());
    }

    private String buildServicePath(String serviceName) {
        return ZKPaths.makePath(BASE_PATH, serviceName);
    }


    private byte[] serializeInstance(ServiceInstance instance) throws Exception{
        return objectMapper.writeValueAsString(instance).getBytes(StandardCharsets.UTF_8);
    }

    private ServiceInstance deserializeInstance(byte[] data) throws Exception{
        return objectMapper.readValue(new String(data, StandardCharsets.UTF_8),ServiceInstance.class);
    }

    private void refreshCache(PathChildrenCache cache) {
        try {
            cache.rebuild();
        } catch (Exception e) {
            throw new RuntimeException("Cache rebuild failed", e);
        }
    }

    @Override
    public void destroy() {
        serviceCaches.values().forEach(cache -> {
            try {
                cache.close();
            } catch (Exception e) {
                logger.error("Failed to close cache", e);
            }
        });
        serviceCaches.clear();
        registeredInstances.clear();
        serviceListenersMap.clear();

        if (client != null) {
            client.close();
        }
    }

    public boolean isConnected() {
        return isConnected && client.getZookeeperClient().isConnected();
    }

}
