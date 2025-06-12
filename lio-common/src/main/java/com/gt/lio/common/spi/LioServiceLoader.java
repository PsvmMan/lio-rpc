package com.gt.lio.common.spi;

import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.common.utils.ByteKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 提供SPI机制，用于获取接口服务实现类
 * @param <T>
 */
public class LioServiceLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(LioServiceLoader.class);

    // 不是所有使用SPI机制的服务都需要配置code，所以默认的code为0x00，代表无效
    public static final byte USELESS_CODE = 0x00;

    // key: code, value: name 例如序列化：code = 0x02, value = hessian
    private  final ConcurrentHashMap<ByteKey, String> CODE_MAP = new ConcurrentHashMap<>();

    // key: name, value: code 例如序列化：name = hessian, value = 0x02
    private  final ConcurrentHashMap<String, Byte> NAME_MAP = new ConcurrentHashMap<>();

    // 接口类
    private final Class<T> service;

    // 服务加载器缓存
    private static final ConcurrentMap<Class<?>, LioServiceLoader<?>> loaderCache = new ConcurrentHashMap<>();

    // 服务实例缓存
    private final ConcurrentMap<String, T> instanceCache = new ConcurrentHashMap();

    // 是否加载过
    private volatile boolean loaded = false;

    private LioServiceLoader(Class<T> service){
        this.service = service;
    }

    /**
     * 获取指定服务的LioServiceLoader实例
     *
     * @param service 服务接口的Class对象
     * @return 返回指定服务的LioServiceLoader实例
     *
     * 此方法用于获取指定服务的LioServiceLoader实例它首先检查服务类型是否为null，
     * 然后检查是否为接口类型最后，它尝试从缓存中获取LioServiceLoader实例如果缓存中没有，
     * 则创建一个新的实例并放入缓存中
     */
    @SuppressWarnings("unchecked")
    public static <T> LioServiceLoader<T> getServiceLoader(Class<T> service) {

        // 检查服务类型是否为null
        if (service == null) {
            throw new IllegalArgumentException("Service type cannot be null.");
        }

        // 检查服务类型是否为接口
        if (!service.isInterface()) {
            throw new IllegalArgumentException("Service type [" + service.getName() + "] must be an interface.");
        }

        // 先从缓存取
        LioServiceLoader<T> loader = (LioServiceLoader<T>) loaderCache.get(service);
        if (loader != null) {
            return loader;
        }

        // 并发创建，只创建一次，细粒度锁
        return (LioServiceLoader<T>) loaderCache.computeIfAbsent(service, key -> {
            if (logger.isInfoEnabled()) {
                logger.info("Creating new LioServiceLoader instance for service: {}", key.getName());
            }
            return new LioServiceLoader<>(key);
        });
    }

    @SuppressWarnings("unchecked")
    public T getService(String name) {

        if(name == null || name.length() == 0){
            throw new IllegalArgumentException("Service name cannot be null.");
        }

        loadInstances();

        T instance = instanceCache.get(name);
        if (instance == null) {
            throw new IllegalArgumentException("No such service(" + service + ") named(" + name + ").");
        }

        return instance;
    }

    private void loadInstances() {
        if(!loaded){
            synchronized (this){
                if(!loaded){
                    ServiceLoader<T> serviceLoader = ServiceLoader.load(service);
                    for (T instance : serviceLoader) {
                        String name = lowercaseFirstLetter(instance.getClass().getSimpleName());
                        Class<?> serviceClass = instance.getClass();
                        if (serviceClass.isAnnotationPresent(SPIService.class)) {
                            SPIService SPIService = serviceClass.getAnnotation(SPIService.class);
                            String value = SPIService.value();
                            if (value != null && !value.isEmpty()) {
                                name = value;
                            }
                            byte code = SPIService.code();
                            if (code != USELESS_CODE) {
                                CODE_MAP.putIfAbsent(new ByteKey(code), name);
                                NAME_MAP.putIfAbsent(name, code);
                            }
                        }
                        instanceCache.put(name, instance);
                    }
                    loaded = true;
                    if (logger.isInfoEnabled()) {
                        logger.info(service + " load finished!");
                    }
                }
            }
        }
    }

    public String lowercaseFirstLetter(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("value cannot be null or empty.");
        }
        StringBuilder sb = new StringBuilder(value);
        // 将首字母小写
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        return sb.toString();
    }

    public String getServiceNameByCode(Byte code) {

        if(code == null){
            throw new IllegalArgumentException("code cannot be null.");
        }

        loadInstances();

        String value = CODE_MAP.get(new ByteKey(code));

        if(value == null){
            throw new IllegalArgumentException("code[" + code + "]," + " no name has been set");
        }

        return value;
    }

    public Byte getCodeByServiceName(String name) {

        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("name cannot be null or empty.");
        }

        loadInstances();

        Byte value = NAME_MAP.get(name);

        if(value == null){
            throw new IllegalArgumentException("name[" + name + "]," + " no code has been set");
        }

        return value;
    }

}
