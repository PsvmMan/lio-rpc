package com.gt.lio.config.spring.context;

import com.gt.lio.cluster.directory.ServiceDirectory;
import com.gt.lio.cluster.invoker.ClusterInvoker;
import com.gt.lio.cluster.invoker.ClusterInvokerFactory;
import com.gt.lio.common.callback.EmptyCallback;
import com.gt.lio.common.spi.LioServiceLoader;
import com.gt.lio.config.*;
import com.gt.lio.config.annotation.LioNoFallback;
import com.gt.lio.config.annotation.LioReferenceMethod;
import com.gt.lio.config.annotation.LioReference;
import com.gt.lio.config.model.LioReferenceMethodMetadata;
import com.gt.lio.limiter.*;
import com.gt.lio.protocol.body.RequestMessage;
import com.gt.lio.protocol.body.ResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gt.lio.common.constants.ClientInvokerConstants.*;
import static com.gt.lio.common.utils.CommonUtils.generateMethodKey;
import static com.gt.lio.config.model.LioReferenceMethodMetadata.DEFAULT;
import static com.gt.lio.config.spring.context.LioServiceRegistrarPostProcessor.checkRegistryConfigs;

public class LioReferenceBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(LioReferenceBeanPostProcessor.class);

    private transient ApplicationContext applicationContext;

    private final AtomicBoolean isLoaded = new AtomicBoolean(false);

    // 注册中心配置
    private List<RegistryConfig> registries;

    // 协议配置
    private Map<String, ProtocolConfig> protocols = new HashMap<>();

    // 消费者配置
    private ConsumerConfig consumerConfig;

    // 应用配置
    private ApplicationConfig application;

    private Map<Class, Map<String, LioReferenceMethodMetadata>> methodsMap = new ConcurrentHashMap<>();

    private Map<Class, Set<String>> fallbackMethodsMap = new ConcurrentHashMap<>();

    private Map<Class, Map<String, LioRateLimitParam>> rateLimitMethodsMap = new ConcurrentHashMap<>();

    private Map<String, Object > proxyObj = new ConcurrentHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        // 加载配置（一次）
        if (isLoaded.compareAndSet(false, true)) {
            loadConfig();
        }

        // 获取当前bean的类对象
        Class<?> clazz = bean.getClass();

        // 遍历当前bean的字段，如果字段标注了LioReference注解，则进行代理注入
        do {
            for (Field field : clazz.getDeclaredFields()) {

                // 判断当前字段是否标注了LioReference注解
                LioReference annotation = field.getAnnotation(LioReference.class);
                if (annotation == null) {
                    continue;
                }

                // 获取字段类型
                Class<?> fieldType = field.getType();

                // 校验字段类型是否是接口
                if (!fieldType.isInterface()) {
                    throw new IllegalArgumentException(
                            "Field [" + field.getName() + "] in class [" + clazz.getName() +
                                    "] is annotated with @LioReference but is not an interface."
                    );
                }

                // 加载方法级别配置
                resolveLioMethods(fieldType);

                // 代理注入
                injectProxy(bean, field, annotation);

            }
            clazz = clazz.getSuperclass();
        } while (clazz != null && !clazz.equals(Object.class));

        return bean;

    }

    private void injectProxy(Object bean, Field field, LioReference annotation) {

        // 保存字段访问权限
        boolean accessible = field.isAccessible();

        try {

            // 设置字段可访问
            if (!accessible) {
                field.setAccessible(true);
            }

            // 创建动态代理对象并注入到字段中
            Object proxy = createProxy(field,annotation);

            field.set(bean, proxy);

        } catch (Exception e) {
            throw new RuntimeException("Failed to inject proxy into field: " + field.getName(), e);
        } finally {
            // 恢复字段访问权限
            field.setAccessible(accessible);
        }

    }

    private  Object createProxy(Field field, LioReference annotation) {

        Class<?> interfaceClass = field.getType();

        String referenceKey = getReferenceKey(interfaceClass, annotation);

        proxyObj.computeIfAbsent(referenceKey, key -> {

            // 加载方法级别配置
            Map<String, LioReferenceMethodMetadata> methods = getReferenceMethods(interfaceClass, annotation);

            // 创建服务目录监听注册中心，动态更新目录数据
            ServiceDirectory serviceDirectory =
                    new ServiceDirectory(annotation, field, application, consumerConfig, registries, protocols, methods);

            // 不同的方法可能要求不同的集群模式，所以需要创建不同的集群调用器
            Map<String, ClusterInvoker> methodClusterInvokers = new HashMap<>();
            Map<String, ClusterInvoker> clusterInvokers = new HashMap<>();
            for (Map.Entry<String, LioReferenceMethodMetadata> entry : methods.entrySet()) {
                String clusterType = entry.getValue().getCluster();
                String methodKey = entry.getKey();
                if (clusterInvokers.containsKey(clusterType)) {
                    methodClusterInvokers.put(methodKey, clusterInvokers.get(clusterType));
                } else {
                    ClusterInvoker clusterInvoker = LioServiceLoader.getServiceLoader(ClusterInvokerFactory.class).getService(clusterType).createInvoker(serviceDirectory, methods);
                    clusterInvokers.put(clusterType, clusterInvoker);
                    methodClusterInvokers.put(methodKey, clusterInvoker);
                }
            }

            // 降级兜底
            Set<String> fallbackMethods = fallbackMethodsMap.get(interfaceClass);
            final Object fallbackObj;
            Class<?> fallbackClass = annotation.fallback();
            if (fallbackClass != Void.class) {
                if (!interfaceClass.isAssignableFrom(fallbackClass)) {
                    throw new IllegalArgumentException("Fallback class [" + fallbackClass.getName() + "] must implement the interface [" + interfaceClass.getName() + "]");
                }
                try {
                    fallbackObj = fallbackClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException("Failed to create fallback instance of class: " + fallbackClass.getName(), e);
                }
            }else {
                fallbackObj = null;
            }

            // 流量控制
            final Map<String, LioRateLimitParam> rateLimitMethods = rateLimitMethodsMap.get(interfaceClass);
            final HashMap<String, RateLimiter> limiters = new HashMap();
            for (Map.Entry<String, LioRateLimitParam> entry : rateLimitMethods.entrySet()) {
                String methodKey = entry.getKey();
                LioRateLimitParam rateLimitParam = entry.getValue();
                RateLimiter rateLimiter = LioServiceLoader.getServiceLoader(RateLimiterFactory.class).getService(rateLimitParam.getType()).getRateLimiter(rateLimitParam);
                limiters.put(methodKey, rateLimiter);
            }

            // 创建代理对象
            Object proxyInstance = Proxy.newProxyInstance(
                    interfaceClass.getClassLoader(),
                    new Class[]{interfaceClass},
                    (proxy, method, args) -> {

                        // 方法名
                        String methodName = method.getName();

                        if (method.getDeclaringClass() == Object.class) {
                            if ("toString".equals(methodName)) {
                                return interfaceClass.getSimpleName() + " proxy@" + Integer.toHexString(System.identityHashCode(proxy));
                            } else if ("hashCode".equals(methodName)) {
                                return System.identityHashCode(proxy);
                            } else {
                                throw new UnsupportedOperationException("Method not supported: " + methodName);
                            }
                        }

                        // 方法key
                        String methodKey = generateMethodKey(methodName, method.getParameterTypes());

                        // 客户端流量限流
                        RateLimiter limiter = limiters.get(methodKey);
                        LioRateLimitParam rateLimitParam = rateLimitMethods.get(methodKey);
                        if (limiter != null && rateLimitParam != null) {
                            if(!rateLimitParam.isBlocking()){
                                if(!limiter.tryAcquire()){
                                    throw new RateLimitException("RPC call rejected due to rate limiting.");
                                }
                            }else if(rateLimitParam.getTimeout() > 0){
                                if(!limiter.acquire(rateLimitParam.getTimeout())){
                                    throw new RateLimitException("RPC call timed out while waiting for rate limit permit.");
                                }
                            }else if(!limiter.acquire()) {
                                throw new RateLimitException("RPC call rejected due to rate limiting.");
                            }
                        }

                        // 如果方法没有对应的集群调用器，则使用默认的集群调用器
                        ClusterInvoker clusterInvoker = methodClusterInvokers.getOrDefault(methodKey, clusterInvokers.get(DEFAULT));

                        // 构建请求消息
                        RequestMessage req = new RequestMessage(serviceDirectory.getServiceName(), methodName, args, method.getParameterTypes(), methodKey);

                        // 发起请求
                        ResponseMessage res = clusterInvoker.invoke(req);

                        // 处理异常
                        if (res.getException() != null) {
                            // 降级兜底处理
                            if (fallbackObj != null && fallbackMethods.contains(methodKey)) {
                                try {
                                    return method.invoke(fallbackObj, args);
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to invoke fallback method: " + method.getName(), e);
                                }
                            }
                            // 抛出异常
                            Throwable cause = res.getException().getCause();
                            throw cause != null ? cause : res.getException();
                        }

                        // 返回结果
                        return res.getResult();
                    }
            );

            if(logger.isInfoEnabled()){
                logger.info("The proxy object was successfully created：{}", referenceKey);
            }

            return proxyInstance;
        });

        return proxyObj.get(referenceKey);

    }

    public static String getReferenceKey(Class<?> interfaceClass, LioReference annotation) {

        StringBuilder sb = new StringBuilder();
        sb.append(interfaceClass.getName()).append(":");

        Method[] methods = annotation.annotationType().getDeclaredMethods();

        for (Method method : methods) {
            try {
                Object value = method.invoke(annotation);
                sb.append(method.getName()).append("=").append(value).append("|");
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke method: " + method.getName(), e);
            }
        }

        return sb.toString();
    }


    private void resolveLioMethods(Class<?> interfaceClass) {

        methodsMap.computeIfAbsent(interfaceClass, k -> {

            Map<String, LioReferenceMethodMetadata> methodMap = new HashMap<>();

            for (Method method : interfaceClass.getDeclaredMethods()) {
                LioReferenceMethod methodAnnotation = method.getAnnotation(LioReferenceMethod.class);
                if (methodAnnotation != null) {
                    methodMap.put(generateMethodKey(method.getName(), method.getParameterTypes()), new LioReferenceMethodMetadata(methodAnnotation));
                }
            }

            return methodMap;
        });

        fallbackMethodsMap.computeIfAbsent(interfaceClass, k -> {

            Set<String> fallbackMethods = new HashSet<>();

            for (Method method : interfaceClass.getDeclaredMethods()) {
                LioNoFallback methodAnnotation = method.getAnnotation(LioNoFallback.class);
                if (methodAnnotation == null) {
                    fallbackMethods.add(generateMethodKey(method.getName(), method.getParameterTypes()));
                }
            }

            return fallbackMethods;
        });

        rateLimitMethodsMap.computeIfAbsent(interfaceClass, k -> {

            Map<String, LioRateLimitParam> methodMap = new HashMap<>();

            for (Method method : interfaceClass.getDeclaredMethods()) {
                LioRateLimit methodAnnotation = method.getAnnotation(LioRateLimit.class);
                if (methodAnnotation != null) {
                    methodMap.put(generateMethodKey(method.getName(), method.getParameterTypes()), new LioRateLimitParam(methodAnnotation));
                }
            }

            return methodMap;
        });
    }


    private Map<String, LioReferenceMethodMetadata> getReferenceMethods(Class<?> interfaceClass, LioReference annotation) {

        Map<String, LioReferenceMethodMetadata> methodMap = new HashMap<>();

        LioReferenceMethodMetadata defaultMethodMetadata = new LioReferenceMethodMetadata();

        if (StringUtils.hasText(annotation.cluster())) {
            defaultMethodMetadata.setCluster(annotation.cluster());
        }else if(StringUtils.hasText(consumerConfig.getCluster())){
            defaultMethodMetadata.setCluster(consumerConfig.getCluster());
        }else {
            defaultMethodMetadata.setCluster(DEFAULT_CLUSTER);
        }

        if (StringUtils.hasText(annotation.loadBalance())) {
            defaultMethodMetadata.setLoadBalance(annotation.loadBalance());
        }else if(StringUtils.hasText(consumerConfig.getLoadbalance())){
            defaultMethodMetadata.setLoadBalance(consumerConfig.getLoadbalance());
        }else {
            defaultMethodMetadata.setLoadBalance(DEFAULT_LOAD_BALANCE);
        }

        if(annotation.retries() > 0){
            defaultMethodMetadata.setRetries(annotation.retries());
        }else if (consumerConfig.getRetries() != null && consumerConfig.getRetries() > 0) {
            defaultMethodMetadata.setRetries(consumerConfig.getRetries());
        }else {
            defaultMethodMetadata.setRetries(DEFAULT_RETRIES);
        }

        if(annotation.parallelNumber() > 0){
            defaultMethodMetadata.setParallelNumber(annotation.parallelNumber());
        }else if (consumerConfig.getParallelNumber() != null && consumerConfig.getParallelNumber() > 0) {
            defaultMethodMetadata.setParallelNumber(consumerConfig.getParallelNumber());
        }else {
            defaultMethodMetadata.setParallelNumber(DEFAULT_PARALLEL_NUMBER);
        }

        if(annotation.timeout() > 0){
            defaultMethodMetadata.setTimeout(annotation.timeout());
        }else if (consumerConfig.getTimeout() != null && consumerConfig.getTimeout() > 0) {
            defaultMethodMetadata.setTimeout(consumerConfig.getTimeout());
        }else {
            defaultMethodMetadata.setTimeout(DEFAULT_TIMEOUT);
        }

        defaultMethodMetadata.setRespond(DEFAULT_RESPOND);
        defaultMethodMetadata.setCompressed(DEFAULT_COMPRESSED);
        defaultMethodMetadata.setCompressionType(DEFAULT_COMPRESSION_TYPE);
        defaultMethodMetadata.setAsync(DEFAULT_ASYNC);

        try {
            defaultMethodMetadata.setCallback(EmptyCallback.class.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to create callback instance of class: " + EmptyCallback.class.getName(), e);
        }

        methodMap.put(DEFAULT, defaultMethodMetadata);

        Map<String, LioReferenceMethodMetadata> temp = methodsMap.get(interfaceClass);
        if(temp != null){
            methodMap.putAll(temp);
        }

        return methodMap;

    }


    private void loadConfig(){

        Map<String, ConsumerConfig> consumerConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ConsumerConfig.class, false, false);
        if (consumerConfigMap != null && consumerConfigMap.size() > 0) {
            for (ConsumerConfig value : consumerConfigMap.values()) {
                this.consumerConfig = value;
            }
        }else {
            this.consumerConfig = new ConsumerConfig();
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
        if (protocolConfigMap != null && protocolConfigMap.size() > 0) {
            for (ProtocolConfig config : protocolConfigMap.values()) {
                if(StringUtils.hasText(config.getName())){
                    protocols.put(config.getName(), config);
                }
            }
        }

        if(logger.isInfoEnabled()){
            logger.info("LioReferenceBeanPostProcessor load config success!");
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
