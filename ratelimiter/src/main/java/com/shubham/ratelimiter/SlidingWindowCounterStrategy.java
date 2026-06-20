package com.shubham.ratelimiter;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SlidingWindowCounterStrategy implements RateLimitStrategy {

    private final StringRedisTemplate redisTemplate;

    // Built once: RedisScript.of computes the SHA so Spring can use EVALSHA.
    private final RedisScript<List> script =
            RedisScript.of(new ClassPathResource("scripts/slidingWindowCounter.lua"), List.class);

    public SlidingWindowCounterStrategy(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Result tryAcquire(String key, int limit, int windowSeconds) {
        long nowMs = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;
        long windowId = nowMs / windowMs;

        // Window-stamped keys so "current" and "previous" are distinct Redis entries.
        String currKey = key + ":" + windowId;
        String prevKey = key + ":" + (windowId - 1);

        // now and window both in ms -> weighting ratio is correct AND PEXPIRE (ms) is correct.
        List<Long> result = redisTemplate.execute(
                script,
                List.of(currKey, prevKey),
                String.valueOf(limit),
                String.valueOf(windowMs),
                String.valueOf(nowMs));

        boolean allowed = result != null && result.get(0) == 1L;
        int remaining = (result == null) ? 0 : Math.max(0, result.get(1).intValue());
        return new Result(allowed, remaining);
    }
}
