package com.gt.lio.cluster.directory;

import com.gt.lio.cluster.client.ClientInvoker;
import com.gt.lio.common.spi.LioServiceLoader;
import com.gt.lio.config.ApplicationConfig;
import com.gt.lio.config.ConsumerConfig;
import com.gt.lio.config.ProtocolConfig;
import com.gt.lio.config.RegistryConfig;
import com.gt.lio.config.annotation.LioReference;
import com.gt.lio.config.model.LioReferenceMethodMetadata;
import com.gt.lio.register.NotifyListener;
import com.gt.lio.register.RegistryFactory;
import com.gt.lio.register.RegistryService;
import com.gt.lio.register.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static com.gt.lio.cluster.client.ClientInvoker.DEFAULT_CONNECTIONS;
import static com.gt.lio.register.ServiceInstance.METADATA_SEPARATOR;

public class ServiceDirectory implements NotifyListener, Directory {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDirectory.class);

    private volatile List<ClientInvoker> clientInvokers = new ArrayList<>();

    private volatile Map<String,HashMap<String, ClientInvoker>> clientInvokersMap = new HashMap<>();

    private ReentrantLock lock = new ReentrantLock();

    // 客户端连接数
    private Integer connections = DEFAULT_CONNECTIONS;

    private Map<String, LioReferenceMethodMetadata> methods;

    Map<String, ProtocolConfig> protocols;

    private String serviceName;

    public ServiceDirectory(LioReference lioReference, Field field, ApplicationConfig application, ConsumerConfig consumerConfig,
                            List<RegistryConfig> registryConfigs, Map<String, ProtocolConfig> protocols, Map<String, LioReferenceMethodMetadata> methods ){

        this.methods = methods;

        this.protocols = protocols;

        // 加载客户端配置
        loadClientConfig(lioReference, consumerConfig);

        // 生成服务名: interfaceName:version:group, 例如：com.gt.lio.demo.api.HelloService:1.0:dev
        serviceName = generateServiceName(field, lioReference, application);

        // 遍历注册中心配置
        for(RegistryConfig registryConfig : registryConfigs){

            // 获取注册中心服务
            RegistryFactory registryFactory = LioServiceLoader.getServiceLoader(RegistryFactory.class).getService(registryConfig.getType());
            RegistryService registryService = registryFactory.getRegistry(registryConfig);

            // 初始化客户端调用列表，key举例: 127.0.0.1:2181
            clientInvokersMap.put(registryConfig.getAddress(), new HashMap<>());

            // 订阅服务
            registryService.subscribe(serviceName, this);

            // 查询服务
            List<ServiceInstance> instances = registryService.getInstances(serviceName);

            // 转换为ClientInvoker
            toClientInvokers(instances, registryConfig.getAddress());
        }

        // 添加钩子，在JVM退出时调用
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            destroy();
        }));
    }

    private void loadClientConfig(LioReference lioReference, ConsumerConfig consumerConfig){

        if(lioReference.connections() > 0){
            this.connections = lioReference.connections();
        }else if(consumerConfig.getConnections() != null && consumerConfig.getConnections() > 0){
            this.connections = consumerConfig.getConnections();
        }

    }

    @Override
    public void notify(List<ServiceInstance> instances,String registerKey) {
        toClientInvokers(instances, registerKey);
    }

    @Override
    public List<ClientInvoker> getClientInvokerList() {
        return clientInvokers;
    }


    public void toClientInvokers(List<ServiceInstance> instances,String registerKey){

        lock.lock();

        try {

            // 定义新的调用列表
            HashMap<String, ClientInvoker> newInvokerHashMap = new HashMap<>();

            // 取出旧的调用列表
            HashMap<String, ClientInvoker> oldInvokerHashMap = clientInvokersMap.get(registerKey);

            if(instances != null && !instances.isEmpty()){
                for(ServiceInstance instance : instances){
                    ClientInvoker clientInvoker = oldInvokerHashMap.get(instance.generateInstanceName());
                    if(clientInvoker == null){
                        clientInvoker = new ClientInvoker(instance, connections, methods, protocols);
                    }
                    newInvokerHashMap.put(instance.generateInstanceName(), clientInvoker);
                }
            }

            // 替换调用列表
            clientInvokersMap.put(registerKey, newInvokerHashMap);
            List<ClientInvoker> temp = new ArrayList<>();

            for (HashMap<String, ClientInvoker> invokerHashMap : clientInvokersMap.values()) {
                for (ClientInvoker invoker : invokerHashMap.values()) {
                    temp.add(invoker);
                }
            }
            clientInvokers = temp;

            // 销毁无用的调用
            for (Map.Entry<String, ClientInvoker> entry : oldInvokerHashMap.entrySet()) {
                if(!newInvokerHashMap.containsKey(entry.getKey())){
                    entry.getValue().destroy();
                }
            }
            oldInvokerHashMap.clear();

            if(logger.isInfoEnabled()){
                logger.info("ServiceDirectory notify success! registerKey: {}", registerKey);
            }

        } finally {
            lock.unlock();
        }

    }

    public String generateServiceName(Field field, LioReference lioReference, ApplicationConfig application) {

        StringBuilder beanNameBuilder = new StringBuilder();
        if(lioReference.interfaceName() != null && !lioReference.interfaceName().isEmpty()){
            beanNameBuilder.append(lioReference.interfaceName());
        }else {
            beanNameBuilder.append(field.getType().getName());
        }

        if(lioReference.version() != null && !lioReference.version().isEmpty()){
            beanNameBuilder.append(METADATA_SEPARATOR).append(lioReference.version());
        }else if(application.getVersion() != null && !application.getVersion().isEmpty()){
            beanNameBuilder.append(METADATA_SEPARATOR).append(application.getVersion());
        }

        if(lioReference.group() != null && !lioReference.group().isEmpty()){
            beanNameBuilder.append(METADATA_SEPARATOR).append(lioReference.group());
        }else if(application.getGroup() != null && !application.getGroup().isEmpty()){
            beanNameBuilder.append(METADATA_SEPARATOR).append(application.getGroup());
        }

        return beanNameBuilder.toString();
    }

    public void destroy() {
        for (ClientInvoker clientInvoker : clientInvokers) {
            clientInvoker.destroy();
        }
        clientInvokers.clear();
        clientInvokersMap.clear();
        if(logger.isInfoEnabled()){
            logger.info("destroy service directory success, the service name is {}", serviceName);
        }
    }

    public String getServiceName(){
        return serviceName;
    }

}
