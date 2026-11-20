package com.fairfold.common.ratelimit;

import com.fairfold.common.errors.ApiException;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Consumes one token from the bucket identified by {@code key}; throws 429
     * with the given retry window when exhausted.
     */
    public void check(String key, int capacity, Duration window, int retryAfterSeconds) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(l -> l.capacity(capacity).refillGreedy(capacity, window))
                .build());
        if (!bucket.tryConsume(1)) {
            throw ApiException.tooManyRequests(retryAfterSeconds);
        }
    }

    /** Clears all buckets; used by tests to isolate rate-limit state. */
    public void reset() {
        buckets.clear();
    }
}
