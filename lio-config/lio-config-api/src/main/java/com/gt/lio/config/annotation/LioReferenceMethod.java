package com.gt.lio.config.annotation;

import com.gt.lio.common.callback.EmptyCallback;
import com.gt.lio.common.callback.RpcCallback;

import java.lang.annotation.*;

import static com.gt.lio.common.constants.ClientInvokerConstants.*;

/**
 * 如果@LioReference接口级别的配置不满足要求，则使用@LioReferenceMethod方法级别配置，@LioReferenceMethod方法级别配置优先级最高
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LioReferenceMethod {

    // 调用超时时间, 默认永不超时
    long timeout() default DEFAULT_TIMEOUT;

    // 调用重试次数, 默认重试2次
    int retries() default DEFAULT_RETRIES;

    // 并发数, 默认3
    int parallelNumber() default DEFAULT_PARALLEL_NUMBER;

    // 是否返回结果, 默认返回
    boolean isRespond() default DEFAULT_RESPOND;

    // 是否异步调用, 默认同步调用
    boolean isAsync() default DEFAULT_ASYNC;

    // 如果是异步调用，可选指定回调类
    Class<? extends RpcCallback> callback() default EmptyCallback.class;

    // 是否压缩, 默认不压缩
    boolean isCompressed() default DEFAULT_COMPRESSED;

    // 压缩类型, 默认zstd
    String compressionType() default DEFAULT_COMPRESSION_TYPE;

    // 调用集群, 默认简单集群, 调用报错直接返回结果
    String cluster() default DEFAULT_CLUSTER;

    // 负载均衡, 默认权重随机
    String loadBalance() default DEFAULT_LOAD_BALANCE;
}
