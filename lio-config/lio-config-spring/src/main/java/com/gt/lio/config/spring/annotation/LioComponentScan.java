package com.gt.lio.config.spring.annotation;

import com.gt.lio.config.spring.context.LioComponentScanRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * lio框架自定义组件的扫描处理
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LioComponentScanRegistrar.class)
public @interface LioComponentScan {

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};
}
