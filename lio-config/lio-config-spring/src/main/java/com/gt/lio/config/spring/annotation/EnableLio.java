package com.gt.lio.config.spring.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 启动LioRPC
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@LioConfigLoad
@LioComponentScan
public @interface EnableLio {

    // 使用 @AliasFor 为 LioComponentScan 的 basePackages 属性提供别名
    // scanBasePackages 是 LioComponentScan 的 basePackages 属性的别名。
    @AliasFor(annotation = LioComponentScan.class, attribute = "basePackages")
    String[] scanBasePackages() default {};

    // 使用 @AliasFor 为 LioComponentScan 的 basePackageClasses 属性提供别名
    // scanBasePackageClasses 是 LioComponentScan 的 basePackageClasses 属性的别名。
    @AliasFor(annotation = LioComponentScan.class, attribute = "basePackageClasses")
    Class<?>[] scanBasePackageClasses() default {};
}
