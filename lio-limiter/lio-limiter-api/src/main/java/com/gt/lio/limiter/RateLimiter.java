package com.gt.lio.limiter;

public interface RateLimiter {
    /**
     * 非阻塞尝试获取许可。
     */
    boolean tryAcquire();

    /**
     * 阻塞方式获取许可，直到超时或成功。
     * @return 是否成功获取
     * @throws InterruptedException 如果线程被中断
     */
    boolean acquire() throws InterruptedException;

    /**
     * 支持指定超时的阻塞获取。
     * @param timeout 超时时间（毫秒）
     * @return 是否成功获取
     * @throws InterruptedException 如果线程被中断
     */
    boolean acquire(long timeout) throws InterruptedException;
}
