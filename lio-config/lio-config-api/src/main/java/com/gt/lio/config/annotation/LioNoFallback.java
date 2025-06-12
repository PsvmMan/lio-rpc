package com.gt.lio.config.annotation;

import java.lang.annotation.*;

import static com.gt.lio.common.constants.ClientInvokerConstants.*;

/**
 * 由于@LioReference的fallback支持接口级别的异常兜底处理，如果某些方法不想受影响，可以添加@LioNoFallback注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LioNoFallback {

}
