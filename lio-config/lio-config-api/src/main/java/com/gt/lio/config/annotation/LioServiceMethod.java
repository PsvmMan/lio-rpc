package com.gt.lio.config.annotation;

import java.lang.annotation.*;

import static com.gt.lio.common.constants.ClientInvokerConstants.*;

/**
 * 服务导出，方法级别配置
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LioServiceMethod {

    // 是否压缩, 默认不压缩
    boolean isCompressed() default DEFAULT_COMPRESSED;

    // 压缩类型, 默认zstd
    String compressionType() default DEFAULT_COMPRESSION_TYPE;

}
