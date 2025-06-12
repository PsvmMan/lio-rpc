package com.gt.lio.limiter.leaky.bucket;

import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.limiter.LioRateLimitParam;
import com.gt.lio.limiter.RateLimiter;
import com.gt.lio.limiter.RateLimiterFactory;

@SPIService(value = "leaky_bucket")
public class LeakyBucketRateLimiterFactory implements RateLimiterFactory {


    @Override
    public RateLimiter getRateLimiter(LioRateLimitParam param) {
        return new LeakyBucketRateLimiter(param.getCapacity(), param.getLeakRate());
    }
}
