package com.shubham.ratelimiter;


import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    private final RateLimitStrategy strategy;

    public RateLimiter(RateLimitStrategy strategy) {
        this.strategy = strategy;
    }

    public RateLimitStrategy.Result tryAcquire(String key, int limit, int windowSeconds) {
        return strategy.tryAcquire(key, limit, windowSeconds);
    }
}
