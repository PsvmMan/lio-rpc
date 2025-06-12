package com.gt.lio.limiter.token.bucket;


import com.gt.lio.limiter.RateLimiter;

/**
 * 令牌桶限流器：控制平均速率 + 支持突发流量。
 */
public class TokenBucketRateLimiter implements RateLimiter {
    private final long capacity;          // 桶容量
    private final long refillTokens;      // 每次补充数量
    private final long intervalMillis;    // 补充间隔（毫秒）
    private long currentTokens; // 当前令牌数量
    private long lastRefillTimestamp; // 上次补充时间戳

    public TokenBucketRateLimiter(long capacity, long refillTokens, long intervalMillis) {
        if (capacity <= 0 || refillTokens <= 0 || intervalMillis <= 0) {
            throw new IllegalArgumentException("capacity and refillTokens and intervalMillis must be greater than 0.");
        }
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.intervalMillis = intervalMillis;
        this.currentTokens = capacity;
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    @Override
    public synchronized boolean tryAcquire() {
        refill();
        if (currentTokens >= 1) {
            currentTokens--;
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean acquire() throws InterruptedException {
        while (!tryAcquire()) {
            wait(100); // 等待一段时间再重试
        }
        return true;
    }

    @Override
    public synchronized boolean acquire(long timeout) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            if (tryAcquire()) {
                return true;
            }
            wait(50); // 小间隔重试
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTimestamp;
        if (elapsed >= intervalMillis) {
            long tokensToAdd = (elapsed / intervalMillis) * refillTokens;
            currentTokens = Math.min(currentTokens + tokensToAdd, capacity);
            lastRefillTimestamp = now;
        }
    }

}
