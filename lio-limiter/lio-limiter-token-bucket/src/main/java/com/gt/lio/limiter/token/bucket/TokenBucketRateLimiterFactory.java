package com.gt.lio.limiter.token.bucket;

import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.limiter.LioRateLimitParam;
import com.gt.lio.limiter.RateLimiter;
import com.gt.lio.limiter.RateLimiterFactory;

@SPIService(value = "token_bucket")
public class TokenBucketRateLimiterFactory implements RateLimiterFactory {


    @Override
    public RateLimiter getRateLimiter(LioRateLimitParam param) {
        return new TokenBucketRateLimiter(param.getCapacity(), param.getRefillTokens(), param.getPeriod());
    }
}
