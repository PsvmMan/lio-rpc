package com.gt.lio.limiter.leaky.bucket;


import com.gt.lio.limiter.RateLimiter;

/**
 * 漏桶限流器：均匀输出，防止突发流量。
 */
public class LeakyBucketRateLimiter implements RateLimiter {
    private final long capacity;           // 桶容量
    private final long leakRate;           // 每秒漏出的请求数
    private long water;                    // 当前水量
    private long lastLeakTimestamp;        // 上次漏水时间

    public LeakyBucketRateLimiter(long capacity, long leakRate) {
        if (capacity <= 0 || leakRate <= 0) {
            throw new IllegalArgumentException("capacity and leakRate must be greater than 0.");
        }
        this.capacity = capacity;
        this.leakRate = leakRate;
        this.water = 0;
        this.lastLeakTimestamp = System.currentTimeMillis();
    }

    @Override
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        long timeElapsed = (now - lastLeakTimestamp) / 1000; // 转换为秒

        if (timeElapsed > 0) {
            water = Math.max(0, water - timeElapsed * leakRate);
            lastLeakTimestamp = now;
        }

        if (water < capacity) {
            water++;
            return true;
        } else {
            return false;
        }
    }


    @Override
    public synchronized boolean acquire() throws InterruptedException {
        while (!tryAcquire()) {
            wait(100);
        }
        return true;
    }

    @Override
    public synchronized boolean acquire(long timeout) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            if (tryAcquire()) {
                return true;
            }
            wait(50);
        }
        return false;
    }

}
