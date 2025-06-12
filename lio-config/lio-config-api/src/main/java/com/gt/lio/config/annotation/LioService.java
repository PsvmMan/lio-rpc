package com.gt.lio.config.annotation;
import java.lang.annotation.*;

/**
 * 服务导出
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface LioService {

    // 服务版本
    String version() default "";

    // 服务分组
    String group() default "";

    // 执行业务的线程池名称
    String threadPoolName() default "";

    // 权重
    int weight() default 5;

}
