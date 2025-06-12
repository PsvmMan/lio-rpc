package com.gt.lio.limiter.sliding.window;

import com.gt.lio.common.annotation.SPIService;
import com.gt.lio.limiter.LioRateLimitParam;
import com.gt.lio.limiter.RateLimiter;
import com.gt.lio.limiter.RateLimiterFactory;

@SPIService(value = "sliding_window")
public class SlidingWindowRateLimiterFactory implements RateLimiterFactory {

    @Override
    public RateLimiter getRateLimiter(LioRateLimitParam param) {
        return new SlidingWindowRateLimiter(param.getPeriod(), param.getLimit());
    }
}
