package com.shubham.ratelimiter;

public interface RateLimitStrategy {

    Result tryAcquire(String key, int limit, int windowSeconds);

    record Result(boolean allowed, int remaining) {}
}
