package com.gt.lio.register.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gt.lio.config.RegistryConfig;
import com.gt.lio.register.NotifyListener;
import com.gt.lio.register.RegistryService;
import com.gt.lio.register.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.Properties;

public class NacosRegistryService implements RegistryService {

    private static final Logger logger = LoggerFactory.getLogger(NacosRegistryService.class);

    public static final int DEFAULT_SESSION_TIMEOUT_MS = 30000;

    private static final String BASE_PATH = "/lio";

    private static final String INSTANCE_KEY = "service-instance";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RegistryConfig config;

    private volatile boolean available = false;

    private volatile boolean initialized = false;

    // Nacos 客户端核心接口
    private NamingService namingService;

    // 已注册的服务实例缓存（用于幂等注册）
    private final Map<String, ServiceInstance> registeredInstances = new ConcurrentHashMap<>();

    // 服务名 -> 监听器集合（每个服务可以有多个监听器）
    private final Map<String, List<NotifyListener>> serviceListenersMap = new ConcurrentHashMap<>();

    // 服务名 -> Nacos 内部监听器（用于 unsubscribe）
    private final Map<String, EventListener> nacosListenersMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService healthCheckExecutor = Executors.newSingleThreadScheduledExecutor();

    public NacosRegistryService(RegistryConfig config) {
        try {

            this.config = config;

            Properties properties = new Properties();
            properties.put("serverAddr", config.getAddress());

            if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                properties.put("username", config.getUsername());
            }
            if (config.getPassword() != null && !config.getPassword().isEmpty()) {
                properties.put("password", config.getPassword());
            }

            this.namingService = NacosFactory.createNamingService(properties);
            this.available = true;
            this.initialized = true;

            startHealthCheck(config.getSessionTimeoutMs() == null ? DEFAULT_SESSION_TIMEOUT_MS : config.getSessionTimeoutMs());

            if(logger.isInfoEnabled()){
                logger.info("Connecting to Nacos server at: {}", config.getAddress());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Nacos server", e);
        }
    }

    @Override
    public void register(ServiceInstance instance) {

        String instanceKey = instance.generateInstanceKey();

        // 幂等注册：避免重复注册
        ServiceInstance previous = registeredInstances.putIfAbsent(instanceKey, instance);
        if (previous != null) {
            logger.warn("Service already registered: {}", instance);
            return;
        }

        try {
            Instance nacosInstance = convertToNacosInstance(instance);
            namingService.registerInstance(nacosInstance.getServiceName(), nacosInstance);
            logger.info("Registered service instance: {}", instanceKey);
        } catch (NacosException e) {
            registeredInstances.remove(instanceKey);
            logger.error("Registration failed for instance: {}", instanceKey, e);
            throw new RuntimeException("Registration failed: " + instanceKey, e);
        }
    }

    @Override
    public void unregister(ServiceInstance instance) {

        String instanceKey = instance.generateInstanceKey();

        try {
            Instance nacosInstance = convertToNacosInstance(instance);
            namingService.deregisterInstance(nacosInstance.getServiceName(), nacosInstance.getIp(), nacosInstance.getPort());
            logger.info("Unregistered service instance: {}", instanceKey);
        } catch (NacosException e) {
            logger.error("Unregistration failed for instance: {}", instanceKey, e);
            throw new RuntimeException("Unregistration failed: " + instanceKey, e);
        } finally {
            registeredInstances.remove(instanceKey);
        }
    }

    @Override
    public void subscribe(String serviceName, NotifyListener listener) {

        nacosListenersMap.computeIfAbsent(serviceName, k -> {
            try {
                // 创建新的 Nacos 监听器
                EventListener newListener = new EventListener() {
                    @Override
                    public void onEvent(Event event) {
                        if (event instanceof NamingEvent) {
                            NamingEvent namingEvent = (NamingEvent) event;
                            List<ServiceInstance> instances = convertFromNacosInstances(namingEvent.getInstances());
                            listener.notify(instances, config.getAddress());
                        }
                    }
                };

                // 注册到 Nacos
                namingService.subscribe(buildServiceName(serviceName), newListener);
                logger.info("Subscribed to service: {}", serviceName);

                return newListener;
            } catch (NacosException e) {
                logger.error("Failed to subscribe to service: {}", serviceName, e);
                throw new RuntimeException("Subscribe failed: " + serviceName, e);
            }
        });

        // 不管监听器是否已存在，都把用户 listener 加入本地 map
        serviceListenersMap.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(listener);

    }

    @Override
    public void unsubscribe(String serviceName, NotifyListener listener) {

        List<NotifyListener> listeners = serviceListenersMap.get(serviceName);
        if (listeners != null) {
            listeners.remove(listener);
        }

        if (listeners == null || listeners.isEmpty()) {
            EventListener eventListener = nacosListenersMap.remove(serviceName);
            if (eventListener != null) {
                try {
                    namingService.unsubscribe(buildServiceName(serviceName), eventListener);
                } catch (NacosException e) {
                    logger.error("Failed to unsubscribe from service: {}", serviceName, e);
                    throw new RuntimeException("Unsubscribe failed: " + serviceName, e);
                }
            }
        }
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) {

        try {
            List<Instance> instances = namingService.getAllInstances(buildServiceName(serviceName));
            return convertFromNacosInstances(instances);
        } catch (NacosException e) {
            logger.error("Failed to get instances of service: {}", serviceName, e);
            throw new RuntimeException("Get instances failed: " + serviceName, e);
        }

    }

    public void startHealthCheck(Integer sessionTimeoutMs) {
        healthCheckExecutor.scheduleAtFixedRate(() -> {

            if (!initialized) return;

            try {
                // 获取一个公共服务（如 "DEFAULT_GROUP" 下的 "nacos.test.service"）测试连接
                String testService = "nacos.test.service";
                namingService.getAllInstances(testService);
                available = true;
            } catch (Exception e) {
                logger.warn("Nacos health check failed", e);
                available = false;
            }
        }, sessionTimeoutMs, sessionTimeoutMs, TimeUnit.MILLISECONDS); // 每5秒检查一次
    }

    @Override
    public void destroy() {
        // 取消所有订阅
        for (String serviceName : new ArrayList<>(serviceListenersMap.keySet())) {
            List<NotifyListener> listeners = serviceListenersMap.remove(serviceName);
            if (listeners != null) {
                for (NotifyListener listener : listeners) {
                    unsubscribe(serviceName, listener);
                }
            }
        }

        // 注销所有已注册的服务
        for (ServiceInstance instance : new ArrayList<>(registeredInstances.values())) {
            unregister(instance);
        }

        if (namingService != null) {
            try {
                namingService.shutDown();
            } catch (NacosException e) {
                throw new RuntimeException(e);
            }
            logger.info("Nacos registry client destroyed.");
        }

        if(healthCheckExecutor != null){
            healthCheckExecutor.shutdown();
        }

    }

    // 将 ServiceInstance 转换为 Nacos Instance
    private Instance convertToNacosInstance(ServiceInstance instance) {
        Instance nacosInstance = new Instance();
        nacosInstance.setIp(instance.getHost());
        nacosInstance.setPort(instance.getPort());
        nacosInstance.setServiceName(buildServiceName(instance));

        try {
            String json = objectMapper.writeValueAsString(instance);
            Map<String, String> metadata = new HashMap<>();
            metadata.put(INSTANCE_KEY, json);
            nacosInstance.setMetadata(metadata);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize ServiceInstance to JSON", e);
            throw new RuntimeException("Failed to serialize ServiceInstance", e);
        }

        nacosInstance.setHealthy(true);
        nacosInstance.setEnabled(true);
        return nacosInstance;
    }

    // 构建符合要求的服务名称
    private String buildServiceName(ServiceInstance instance) {
        return BASE_PATH + "/" + instance.getServiceName();
    }

    private String buildServiceName(String serviceName) {
        return BASE_PATH + "/" + serviceName;
    }

    // 将 Nacos Instance 转换为 ServiceInstance
    private List<ServiceInstance> convertFromNacosInstances(List<Instance> nacosInstances) {
        List<ServiceInstance> result = new ArrayList<>();
        for (Instance instance : nacosInstances) {
            String json = instance.getMetadata().get(INSTANCE_KEY);
            if (json == null || json.isEmpty()) {
                continue;
            }
            try {
                ServiceInstance si = objectMapper.readValue(json, ServiceInstance.class);
                result.add(si);
            } catch (Exception e) {
                logger.warn("Failed to deserialize ServiceInstance from metadata", e);
            }
        }
        return result;
    }

    public boolean isConnected() {
        return available;
    }

}
