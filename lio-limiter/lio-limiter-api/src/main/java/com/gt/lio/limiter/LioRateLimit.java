package com.gt.lio.limiter;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LioRateLimit {

    /**
     * 限流器类型，默认是令牌桶。
     */
    String type() default "token_bucket";

    /**
     * 每单位时间允许的最大请求数。(滑动窗口)
     */
    long limit() default 0;

    /**
     * 单位时间长度（毫秒），如 1000 表示每秒最多 limit 次请求。(令牌桶、滑动窗口)
     */
    long period() default 0;

    /**
     * 容量。(令牌桶、漏桶)
     */
    long capacity() default 0;

    /**
     * 每次补充的令牌数。(令牌桶)
     */
    long refillTokens() default 0;

    /**
     * 漏桶漏出速率，每秒处理多少请求（漏桶）。
     */
    long leakRate() default 0;

    /**
     * 等待限流释放的时间（毫秒），用于阻塞式限流。(用于 acquire 方法)
     */
    long timeout() default 0;

    /**
     * 是否启用阻塞等待模式。
     */
    boolean blocking() default false;

    /**
     * 备用字段1
     */
    String field1() default "";

    /**
     * 备用字段2
     */
    String field2() default "";

    /**
     * 备用字段3
     */
    String field3() default "";
}
