package com.shubham.ratelimiter;

import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sliding-window log via a Redis sorted set (ClassDojo algorithm, penalty mode).
 *
 * Every attempt is recorded — including rejected ones — so sustained hammering
 * keeps the window full and extends the block until the client backs off.
 *
 * Implemented with a MULTI/EXEC transaction (no Lua): the trim → add → count → ttl
 * commands run atomically in one round trip. Because penalty mode ALWAYS inserts,
 * there's no need to branch mid-transaction — we read the count after EXEC and
 * decide allow/reject in Java.
 */
@Component
@Primary
public class SortedSetStrategy implements RateLimitStrategy {

    private final StringRedisTemplate redisTemplate;

    // Disambiguates members that share the same microsecond timestamp under concurrency,
    // so two simultaneous requests don't collapse into one ZSET member (undercount).
    private final AtomicLong sequence = new AtomicLong();

    public SortedSetStrategy(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Result tryAcquire(String key, int limit, int windowSeconds) {
        long nowMicros = System.currentTimeMillis() * 1000L;
        long windowMicros = windowSeconds * 1_000_000L;
        long cutoff = nowMicros - windowMicros;
        //appens something unique to the jvm
        String member = nowMicros + ":" + sequence.incrementAndGet();

        @SuppressWarnings({"unchecked", "rawtypes"})
        List<Object> results = redisTemplate.execute(new SessionCallback<>() {
            @Override
            public List<Object> execute(org.springframework.data.redis.core.RedisOperations operations) {
                operations.multi();
                // 1. Drop everything older than one window ago.
                operations.opsForZSet().removeRangeByScore(key, 0, cutoff);
                // 2. Record this attempt (always — even if it ends up rejected).
                operations.opsForZSet().add(key, member, nowMicros);
                // 3. Count what's in the window (includes the attempt just added).
                operations.opsForZSet().zCard(key);
                // 4. Refresh TTL so idle keys self-clean.
                operations.expire(key, Duration.ofSeconds(windowSeconds));
                return operations.exec();
            }
        });

        // results = [removedCount, addedFlag, zcardCount, expireFlag]
        long count = (results == null) ? 0 : ((Number) results.get(2)).longValue();

        boolean allowed = count <= limit;
        int remaining = allowed ? (int) Math.max(0, limit - count) : 0;
        return new Result(allowed, remaining);
    }
}
