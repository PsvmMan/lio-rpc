package com.gt.lio.config.spring.annotation;

import com.gt.lio.config.spring.context.LioConfigRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 加载lio配置信息到spring容器中
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LioConfigRegistrar.class)
public @interface LioConfigLoad {

}
