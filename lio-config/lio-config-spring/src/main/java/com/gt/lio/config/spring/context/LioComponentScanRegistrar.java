package com.gt.lio.config.spring.context;

import com.gt.lio.config.spring.annotation.LioComponentScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

public class LioComponentScanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(LioComponentScanRegistrar.class);

    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        // 获取扫描包
        Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);

        // 处理@LioService注解
        registerServiceRegistrarPostProcessor(packagesToScan, registry);

        // 处理@LioReference注解
        registerReferenceBeanPostProcessor(registry, LioReferenceBeanPostProcessor.class);

    }

    private void registerServiceRegistrarPostProcessor(Set<String> packagesToScan, BeanDefinitionRegistry registry) {

        BeanDefinitionBuilder builder = rootBeanDefinition(LioServiceRegistrarPostProcessor.class);
        builder.addConstructorArgValue(packagesToScan);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);

        if (logger.isInfoEnabled()) {
            logger.info("Registered LioServiceRegistrarPostProcessor");
        }

    }

    private void registerReferenceBeanPostProcessor(BeanDefinitionRegistry registry, Class<?> beanType) {

        RootBeanDefinition beanDefinition = new RootBeanDefinition(beanType);

        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);

        if (logger.isInfoEnabled()) {
            logger.info("Registered " + beanType.getName() + " as a bean.");
        }

    }

    private Set<String> getPackagesToScan(AnnotationMetadata metadata) {

        // 从导入类的注解元数据中提取 @LioComponentScan 注解的属性
        // metadata 是触发当前 ImportBeanDefinitionRegistrar 的类的注解信息
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(LioComponentScan.class.getName(), false));

        // 获取注解的属性值，如果没有配置，则返回默认值
        String[] basePackages = attributes.getStringArray("basePackages");
        Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");

        Set<String> packagesToScan = new LinkedHashSet<>();

        for (String pkg : basePackages) {
            String resolvedPkg = environment.resolvePlaceholders(pkg.trim());
            if (StringUtils.hasText(resolvedPkg)) {
                packagesToScan.add(resolvedPkg);
            }
        }

        for (Class<?> clazz : basePackageClasses) {
            String packageName = ClassUtils.getPackageName(clazz);
            if (StringUtils.hasText(packageName)) {
                packagesToScan.add(packageName);
            }
        }

        // 如果都没有配置，则使用当前注解类所在的包作为默认包
        if (packagesToScan.isEmpty()) {
            String className = metadata.getClassName();
            String packageName = ClassUtils.getPackageName(className);
            return Collections.singleton(packageName);
        }

        return packagesToScan;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
