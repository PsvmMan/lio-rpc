package com.gt.lio.limiter.sliding.window;

import com.gt.lio.limiter.RateLimiter;

import java.util.LinkedList;

/**
 * 滑动窗口限流器：精确控制单位时间内的请求数。
 */
public class SlidingWindowRateLimiter implements RateLimiter {
    private final long windowSizeInMillis;
    private final long maxRequestsInWindow;
    private final LinkedList<Long> requestTimestamps = new LinkedList<>();

    public SlidingWindowRateLimiter(long windowSizeInMillis, long maxRequestsInWindow) {
        if (windowSizeInMillis <= 0 || maxRequestsInWindow <= 0) {
            throw new IllegalArgumentException("windowSizeInMillis and maxRequestsInWindow must be greater than 0.");
        }
        this.windowSizeInMillis = windowSizeInMillis;
        this.maxRequestsInWindow = maxRequestsInWindow;
    }

    @Override
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();

        // 移除窗口外的请求记录
        while (!requestTimestamps.isEmpty() && now - requestTimestamps.peek() > windowSizeInMillis) {
            requestTimestamps.poll();
        }

        if (requestTimestamps.size() < maxRequestsInWindow) {
            requestTimestamps.add(now);
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
