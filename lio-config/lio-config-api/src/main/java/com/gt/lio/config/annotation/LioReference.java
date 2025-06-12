package com.gt.lio.config.annotation;


import java.lang.annotation.*;

/**
 * 服务引用
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface LioReference {

    // 服务提供端接口的全限定名，默认为当前注解所在属性的类型的全限定名，但是服务提供方和服务引用方有时候是不一致的，可以通过该属性指定
    String interfaceName() default "";

    // 服务版本号
    String version() default "";

    // 服务分组
    String group() default "";

    // 调用超时时间
    long timeout() default 0;

    // 重试次数
    int retries() default 0;

    // 并发调用模式下的并发数
    int parallelNumber() default 0;

    // 集群模式
    String cluster() default "";

    // 负载均衡算法
    String loadBalance() default "";

    // 客户端连接数
    int connections() default 0;

    Class<?> fallback() default Void.class; // 默认不启用

}
