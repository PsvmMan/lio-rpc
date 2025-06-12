package com.gt.lio.limiter;

public class LioRateLimitParam {

    /**
     * 限流器类型，默认是令牌桶。
     */
    private String type;

    /**
     * 每单位时间允许的最大请求数。(滑动窗口)
     */
    private long limit;

    /**
     * 单位时间长度（毫秒），如 1000 表示每秒最多 limit 次请求。(令牌桶、滑动窗口)
     */
    private long period;

    /**
     * 容量。(令牌桶、漏桶)
     */
    private long capacity;

    /**
     * 每次补充的令牌数。(令牌桶)
     */
    private long refillTokens;

    /**
     * 漏桶漏出速率，每秒处理多少请求（漏桶）。
     */
    private long leakRate;

    /**
     * 等待限流释放的时间（毫秒），用于阻塞式限流。(用于 acquire 方法)
     */
    private long timeout;

    /**
     * 是否启用阻塞等待模式。
     */
    private boolean blocking;

    /**
     * 备用字段1
     */
    private String field1;

    /**
     * 备用字段2
     */
    private String field2;

    /**
     * 备用字段3
     */
    private String field3;

    public LioRateLimitParam(LioRateLimit lioRateLimit){
        this.type = lioRateLimit.type();
        this.limit = lioRateLimit.limit();
        this.period = lioRateLimit.period();
        this.capacity = lioRateLimit.capacity();
        this.refillTokens = lioRateLimit.refillTokens();
        this.leakRate = lioRateLimit.leakRate();
        this.timeout = lioRateLimit.timeout();
        this.blocking = lioRateLimit.blocking();
        this.field1 = lioRateLimit.field1();
        this.field2 = lioRateLimit.field2();
        this.field3 = lioRateLimit.field3();
    }

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    public String getField3() {
        return field3;
    }

    public void setField3(String field3) {
        this.field3 = field3;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(long refillTokens) {
        this.refillTokens = refillTokens;
    }

    public long getLeakRate() {
        return leakRate;
    }

    public void setLeakRate(long leakRate) {
        this.leakRate = leakRate;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }
}
