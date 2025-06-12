package com.gt.lio.config.spring.context;

import com.gt.lio.common.spi.LioServiceLoader;
import com.gt.lio.common.utils.AddressUtils;
import com.gt.lio.config.ApplicationConfig;
import com.gt.lio.config.ProtocolConfig;
import com.gt.lio.config.ProviderConfig;
import com.gt.lio.config.RegistryConfig;
import com.gt.lio.config.annotation.LioService;
import com.gt.lio.config.annotation.LioServiceMethod;
import com.gt.lio.config.model.LioServiceMethodMetadata;
import com.gt.lio.common.invoker.RpcInvokerProvider;
import com.gt.lio.common.threadpool.ThreadPoolProvider;
import com.gt.lio.common.constants.ThreadPoolConstants;
import com.gt.lio.common.threadpool.ThreadPoolFactory;
import com.gt.lio.register.RegistryFactory;
import com.gt.lio.register.RegistryService;
import com.gt.lio.register.ServiceInstance;
import com.gt.lio.remote.ServerFactory;
import com.gt.lio.remote.param.ServerStartParam;
import com.gt.lio.serialization.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.gt.lio.common.constants.ClientInvokerConstants.DEFAULT_COMPRESSED;
import static com.gt.lio.common.constants.ClientInvokerConstants.DEFAULT_COMPRESSION_TYPE;
import static com.gt.lio.common.utils.CommonUtils.generateMethodKey;
import static com.gt.lio.config.model.LioServiceMethodMetadata.DEFAULT;
import static com.gt.lio.common.constants.ProtocolConstants.DEFAULT_PROTOCOL_TYPE;
import static com.gt.lio.common.constants.RpcInvokerConstants.DEFAULT_INVOKER_TYPE;
import static com.gt.lio.remote.RemoteConstants.DEFAULT_REMOTE_TYPE;
import static com.gt.lio.serialization.SerializationConstants.DEFAULT_SERIALIZATION_TYPE;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

public class LioServiceRegistrarPostProcessor implements BeanDefinitionRegistryPostProcessor, BeanPostProcessor, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(LioServiceRegistrarPostProcessor.class);

    private static final String SEPARATOR = ":";

    private final Set<String> packagesToScan;

    private final Map<String, Boolean> exportedServiceKeys = new ConcurrentHashMap<>();

    private transient ApplicationContext applicationContext;

    // 服务提供者配置
    private ProviderConfig provider;

    // 应用配置
    private ApplicationConfig application;

    // 注册中心配置
    private List<RegistryConfig> registries;

    // 协议配置
    private List<ProtocolConfig> protocols;

    public LioServiceRegistrarPostProcessor(Set<String> packagesToScan) {
        this.packagesToScan = packagesToScan;
    }


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        if(!CollectionUtils.isEmpty(packagesToScan)){
            registerServiceBeans(packagesToScan, registry);
        }else{
            if (logger.isInfoEnabled()) {
                logger.info("No packagesToScan found, no service bean has been registered");
            }
        }

    }

    /**
     * 将使用了@LioService 注解的类注册为bean
     */
    private void registerServiceBeans(Set<String> packagesToScan, BeanDefinitionRegistry registry) {

        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(LioService.class));

        for (String basePackage : packagesToScan) {

            scanner.scan(basePackage);

            if(logger.isInfoEnabled()){
                logger.info("The BeanDefinition of [ Classes annotated with LioService ] has been registered with base package : " + basePackage);
            }
        }

    }

    /**
     * 对使用@LioService注解的bean进行后置处理，进行服务导出
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        // 只加载一次配置
        if (exportedServiceKeys.putIfAbsent("*", Boolean.TRUE) == null) {
            loadConfig();
        }

        Class<?> beanClass = bean.getClass();

        LioService service = findAnnotation(beanClass, LioService.class);

        // 当前bean使用了LioService注解 且 服务需要导出
        if (service != null && provider.getExport()) {

            // 获取接口类
            Class<?> interfaceClass = getInterfaceClass(beanClass);

            // 生成服务唯一键，一个接口同分组同版本只能导出一次，例如：com.gt.lio.demo.api.DemoService:1.0:dev
            String serviceKey = generateServiceKey(service, interfaceClass);

            // 判断是否已经导出过，如果已经导出过，则不进行导出
            if (exportedServiceKeys.putIfAbsent(serviceKey, Boolean.TRUE) == null) {
                exportService(service, bean, serviceKey, interfaceClass);
            }

        }
        return bean;
    }


    private void exportService(LioService service, Object bean, String serviceKey, Class<?> interfaceClass){

        // 遍历协议配置
        if(protocols != null && !protocols.isEmpty()){
            for (ProtocolConfig protocol : protocols) {
                exportAndRegister(protocol, service, bean, serviceKey, interfaceClass);
            }
        }else {
            throw new RuntimeException("There is no available protocol configuration！");
        }

    }

    private void exportAndRegister(ProtocolConfig protocolConfig, LioService service, Object bean, String serviceKey, Class<?> interfaceClass){

        // 创建服务实例对象
        ServiceInstance instance = createServiceInstance(protocolConfig, service, serviceKey);

        // 构建业务线程池
        buildThreadPool(instance, service);

        // 构建当前服务的执行对象
        buildInvoker(serviceKey, bean, interfaceClass);

        // 启动远程传输服务
        startRemoteServer(instance, protocolConfig);

        // 注册服务
        for(RegistryConfig registryConfig : registries){
            RegistryFactory registryFactory = LioServiceLoader.getServiceLoader(RegistryFactory.class).getService(registryConfig.getType());
            RegistryService registryService = registryFactory.getRegistry(registryConfig);
            registryService.register(instance);
        }

        if (logger.isInfoEnabled()) {
            logger.info("The service has been exported, serviceKey : " + serviceKey);
        }
    }


    /**
     * 启动传输服务，同一个 IP:端口 的服务不会重复启动
     */
    private void startRemoteServer(ServiceInstance instance, ProtocolConfig protocolConfig){
        // 传输层默认使用netty
        String remote = protocolConfig.getRemote();
        if(remote == null || remote.isEmpty()){
            remote = DEFAULT_REMOTE_TYPE;
        }
        ServerFactory serverFactory = LioServiceLoader.getServiceLoader(ServerFactory.class).getService(remote);
        ServerStartParam param = new ServerStartParam(instance.getHost(),instance.getPort(), protocolConfig.getHeartbeatReadTimeout(), instance.getProtocol());
        serverFactory.startServer(param);
        instance.getMetadata().put("remote",remote);
    }

    /**
     * 我们需要将ref转化为可执行对象，因为当我们拿到请求的方法名、方法参数、方法参数类型的时候，
     * 需要通过方法名、方法参数类型来获取目标对象的方法，然后执行，有很多步骤，所以这里需要将ref转化为可执行对象
     * 默认使用cglib代理方式，也可以自定义代理方式，只需要实现InvokerFactory接口，然后通过SPI机制暴露服务，服务端会自动发现并加载
     * 用户可以通过lio.provider.proxy=cglib的方式来配置代理方式
     * @param key 接口全限定名称:版本:分组，例如：com.gt.lio.service.HelloService:1.0:dev
     * @param target 目标对象，例如：com.gt.lio.service.impl.HelloServiceImpl
     */
    private void buildInvoker(String key, Object target, Class<?> interfaceClass){

        Map<String, LioServiceMethodMetadata> methods = resolveLioMethods(interfaceClass);

        String proxy = provider.getProxy();
        if(!StringUtils.hasText(proxy)){
            proxy = DEFAULT_INVOKER_TYPE;
        }

        // Invoker携带元数据
        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("methods",methods);
        RpcInvokerProvider.instance.buildInvoker(key, target, metadata, proxy);
    }

    private Map<String, LioServiceMethodMetadata> resolveLioMethods(Class<?> interfaceClass) {

        Map<String, LioServiceMethodMetadata> methodMap = new HashMap<>();

        for (Method method : interfaceClass.getDeclaredMethods()) {
            LioServiceMethod methodAnnotation = method.getAnnotation(LioServiceMethod.class);
            if (methodAnnotation != null) {
                methodMap.put(generateMethodKey(method.getName(), method.getParameterTypes()), new LioServiceMethodMetadata(methodAnnotation));
            }
        }

        // 添加一个默认的
        LioServiceMethodMetadata lioServiceMethodMetadata = new LioServiceMethodMetadata();
        lioServiceMethodMetadata.setCompressed(DEFAULT_COMPRESSED);
        lioServiceMethodMetadata.setCompressionType(DEFAULT_COMPRESSION_TYPE);
        methodMap.put(DEFAULT, lioServiceMethodMetadata);

        return methodMap;
    }

    /**
     * 使用@LioService注解，可以指定业务执行的线程池名称，如果没有指定，则使用默认线程池名称'default'
     * 用户也可以自定义线程池，只需要实现ThreadPoolProvider接口，然后通过SPI机制暴露服务，服务端会自动发现并加载
     * 用户可以通过lio.provider.threadPoolxxxx=xxx的方式来配置default线程池参数
     */
    private void buildThreadPool(ServiceInstance instance, LioService service){

        // 获取业务线程池名称
        String threadPoolName = service.threadPoolName();

        // 或许线程池参数
        Map<String,Integer> threadPoolParams = new HashMap();
        threadPoolParams.put("threadPoolMaxSize",provider.getDefaultThreadPoolMaxSize());
        threadPoolParams.put("threadPoolCoreSize",provider.getDefaultThreadPoolCoreSize());
        threadPoolParams.put("threadPoolKeepAliveTime",provider.getDefaultThreadPoolKeepAliveTime());
        threadPoolParams.put("threadPoolQueueSize",provider.getDefaultThreadPoolQueueSize());

        // 如果没有配置线程池名称，则使用默认线程池'default'
        if(!StringUtils.hasText(threadPoolName)){
            threadPoolName = ThreadPoolConstants.defaultThreadPoolName;
        }

        // 构建线程池
        ThreadPoolProvider.instance.buildThreadPool(threadPoolName, threadPoolParams);

        // 获取线程池编码, 放入元数据中
        Byte threadPoolCode = LioServiceLoader.getServiceLoader(ThreadPoolFactory.class).getCodeByServiceName(threadPoolName);
        instance.getMetadata().put("threadPoolName",threadPoolCode);
    }


    private ServiceInstance createServiceInstance(ProtocolConfig protocolConfig, LioService service, String serviceKey){

        // 协议名称，默认协议为lio
        String protocolName = protocolConfig.getName();
        if(!StringUtils.hasText(protocolName)){
            protocolName = DEFAULT_PROTOCOL_TYPE;
        }
        // 序列化方法，默认为hessian
        String serialization = protocolConfig.getSerialization();
        if(!StringUtils.hasText(serialization)){
            serialization = DEFAULT_SERIALIZATION_TYPE;
        }

        // 创建实例
        ServiceInstance instance = new ServiceInstance();
        instance.setServiceName(serviceKey);
        instance.setProtocol(protocolName);
        instance.setHost(AddressUtils.getServerIpAddress());
        instance.setPort(AddressUtils.validatePort(protocolConfig.getPort()));
        instance.setMetadata(new HashMap<>());
        instance.getMetadata().put("serialization", serialization);

        Byte serializationCode = LioServiceLoader.getServiceLoader(Serialization.class).getCodeByServiceName(serialization);
        instance.getMetadata().put("serializationCode",serializationCode);

        int weight = service.weight();
        if(weight < 1 || weight > 10){
            throw new IllegalArgumentException("weight must be between 1 and 10");
        }
        instance.getMetadata().put("weight", weight);

        return instance;
    }


    private void loadConfig(){

        Map<String, ProviderConfig> providerConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProviderConfig.class, false, false);
        if (providerConfigMap != null && providerConfigMap.size() > 0) {
            for (ProviderConfig value : providerConfigMap.values()) {
                this.provider = value;
            }
        }else {
            this.provider = new ProviderConfig();
        }

        Map<String, ApplicationConfig> applicationConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ApplicationConfig.class, false, false);
        if (applicationConfigMap != null && applicationConfigMap.size() > 0) {
            for (ApplicationConfig value : applicationConfigMap.values()) {
                this.application = value;
            }
        }else {
            this.application = new ApplicationConfig();
        }

        Map<String, RegistryConfig> registryConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RegistryConfig.class, false, false);
        List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
        if (registryConfigMap != null && registryConfigMap.size() > 0) {
            for (RegistryConfig value : registryConfigMap.values()) {
                registryConfigs.add(value);
            }
        }
        this.registries = checkRegistryConfigs(registryConfigs);

        Map<String, ProtocolConfig> protocolConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class, false, false);
        List<ProtocolConfig> protocolConfigs = new ArrayList<ProtocolConfig>();
        if (protocolConfigMap != null && protocolConfigMap.size() > 0) {
            for (ProtocolConfig value : protocolConfigMap.values()) {
                protocolConfigs.add(value);
            }
        }
        this.protocols = protocolConfigs;

        if(logger.isInfoEnabled()){
            logger.info("LioServiceRegistrarPostProcessor load config success.");
        }
    }

    public static List<RegistryConfig> checkRegistryConfigs(List<RegistryConfig> registries){
        HashSet<String> set = new HashSet<>();
        ArrayList<RegistryConfig> list = new ArrayList<>();
        if(registries != null && registries.size() > 0){
            for(RegistryConfig registryConfig : registries){
                if(!StringUtils.hasText(registryConfig.getType())){
                    continue;
                }
                if(!AddressUtils.isValidIpPort(registryConfig.getAddress())){
                    continue;
                }
                if(set.contains(registryConfig.getType() + registryConfig.getAddress())){
                    continue;
                }

                list.add(registryConfig.clone());
                set.add(registryConfig.getType() + registryConfig.getAddress());
            }
        }
        if(list.isEmpty()){
            throw new RuntimeException("There is no available registry configuration！");
        }
        return list;
    }

    private String generateServiceKey(LioService service, Class<?> interfaceClass) {

        StringBuilder beanNameBuilder = new StringBuilder(interfaceClass.getName());

        if (StringUtils.hasText(service.version())) {
            beanNameBuilder.append(SEPARATOR).append(service.version());
        }else if(StringUtils.hasText(application.getVersion())){
            beanNameBuilder.append(SEPARATOR).append(application.getVersion());
        }

        if (StringUtils.hasText(service.group())) {
            beanNameBuilder.append(SEPARATOR).append(service.group());
        }else if(StringUtils.hasText(application.getGroup())){
            beanNameBuilder.append(SEPARATOR).append(application.getGroup());
        }

        return beanNameBuilder.toString();
    }

    private Class<?> getInterfaceClass(Class<?> beanClass){

        Class<?> interfaceClass = null;

        Class<?>[] allInterfaces = beanClass.getInterfaces();

        if (allInterfaces.length > 0) {
            interfaceClass = allInterfaces[0];
        }

        if(interfaceClass == null){
            throw new IllegalArgumentException("Classes using the LioService annotation must implement the interface ! ");
        }

        return interfaceClass;
    }
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


}
