package com.gt.lio.config.spring.context;

import com.gt.lio.config.*;
import com.gt.lio.config.spring.utils.LioPropertyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;


import java.util.*;



/**
 * 将配置信息注册为BeanDefinition, 并注册到 Spring 容器中
 */
public class LioConfigRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(LioConfigRegistrar.class);

    private ConfigurableEnvironment environment;

    private final List<Map<String, Object>> configurations;

    public LioConfigRegistrar() {
        this.configurations =  Collections.unmodifiableList(
                Arrays.asList(
                        createConfigMap("lio.application", ApplicationConfig.class, false),
                        createConfigMap("lio.registry", RegistryConfig.class, false),
                        createConfigMap("lio.protocol", ProtocolConfig.class, false),
                        createConfigMap("lio.provider", ProviderConfig.class, false),
                        createConfigMap("lio.consumer", ConsumerConfig.class, false),
                        createConfigMap("lio.registries", RegistryConfig.class, true),
                        createConfigMap("lio.protocols", ProtocolConfig.class, true)
                )
        );
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        for (Map<String, Object> config : configurations) {
            registerConfigurationBeanDefinitions(config, registry);
        }

    }

    private void registerConfigurationBeanDefinitions(Map<String, Object> config, BeanDefinitionRegistry registry) {

        String prefix = (String) config.get("prefix");

        Class<?> type = (Class<?>) config.get("type");

        boolean multiple = (Boolean) config.get("multiple");

        if (multiple) {
            registerMultipleBeans(prefix, type, registry);
        } else {
            registerSingleBean(prefix, type, registry);
        }
    }

    /**
     * 注册单一 Bean
     */
    private void registerSingleBean(String prefix, Class<?> clazz, BeanDefinitionRegistry registry) {

        Map<String, Object> props = getPropertiesByPrefix(prefix + ".");

        if (props.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("No configuration properties found for prefix '{}'", prefix);
            }
            return;
        }

        String beanName = generateUniqueBeanName(clazz.getSimpleName(), registry);

        registerBean(clazz, registry, props, beanName);
    }

    /**
     * 注册多个 Bean（如 lio.protocols.*, lio.registries.*）
     */
    private void registerMultipleBeans(String prefix, Class<?> clazz, BeanDefinitionRegistry registry) {

        Set<String> keys = getAllKeysStartingWith(prefix + ".");

        Map<String, Map<String, Object>> grouped = new HashMap<>();

        for (String key : keys) {
            String[] parts = key.split("\\.", 4); // lio.protocols.p1.name => [lio, protocols, p1, name]
            if (parts.length >= 4) {
                String groupKey = parts[2];
                String subKey = parts[3];
                grouped.computeIfAbsent(groupKey, k -> new HashMap<>())
                        .put(subKey, environment.getProperty(key));
            }
        }

        grouped.forEach((name, props) -> {

            String baseBeanName = clazz.getSimpleName() + "_" + name;
            String beanName = generateUniqueBeanName(baseBeanName, registry);

            registerBean(clazz, registry, props, beanName);
        });
    }

    private void registerBean(Class<?> clazz, BeanDefinitionRegistry registry, Map<String, Object> props, String beanName){
        Object instance = LioPropertyMapper.map(props, clazz);
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClassName(clazz.getName());
        bd.setInstanceSupplier(() -> instance);
        registry.registerBeanDefinition(beanName, bd);

        if (logger.isInfoEnabled()) {
            logger.info("The BeanDefinition[ class : " + clazz.getSimpleName() + ", name : " + beanName +
                    " ]" +  " has been registered");
        }
    }

    /**
     * 获取指定前缀的所有属性
     */
    private Map<String, Object> getPropertiesByPrefix(String prefix) {
        Set<String> allKeys = getAllKeysStartingWith(prefix);
        Map<String, Object> result = new HashMap<>();

        for (String fullKey : allKeys) {
            // 截取 prefix 后面的部分作为子 key
            String subKey = fullKey.substring(prefix.length());
            // 获取对应的值
            String value = environment.getProperty(fullKey);
            // 放入结果 map
            result.put(subKey, value);
        }

        return result;
    }

    /**
     * 获取所有以指定前缀开头的键
     */
    private Set<String> getAllKeysStartingWith(String prefix) {
        Set<String> result = new HashSet<>();
        environment.getPropertySources().forEach(ps -> {
            if (ps instanceof org.springframework.core.env.EnumerablePropertySource) {
                String[] names = ((org.springframework.core.env.EnumerablePropertySource) ps).getPropertyNames();
                for (String name : names) {
                    if (name.startsWith(prefix)) {
                        result.add(name);
                    }
                }
            }
        });
        return result;
    }

    /**
     * 生成唯一的 Bean 名称
     */
    private String generateUniqueBeanName(String baseName, BeanDefinitionRegistry registry) {

        int counter = 0;
        String candidateName = baseName;

        while (registry.containsBeanDefinition(candidateName)) {
            counter++;
            candidateName = baseName + "_" + counter;
        }

        return candidateName;
    }

    @Override
    public void setEnvironment(Environment environment) {
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
        this.environment = (ConfigurableEnvironment) environment;
    }

    private Map<String, Object> createConfigMap(String prefix, Class<?> type, boolean multiple) {
        Map<String, Object> map = new HashMap<>();
        map.put("prefix", prefix);
        map.put("type", type);
        map.put("multiple", multiple);
        return Collections.unmodifiableMap(map);
    }
}
