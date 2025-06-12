package com.gt.lio.limiter;

public interface RateLimiterFactory {

    RateLimiter getRateLimiter(LioRateLimitParam param);
}
