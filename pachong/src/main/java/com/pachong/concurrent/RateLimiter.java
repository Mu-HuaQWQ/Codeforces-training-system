package com.pachong.concurrent;

import com.pachong.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 令牌桶限流器 — 按平台限流
 *
 * 使用 ReentrantLock + Condition 实现，不依赖外部库
 */
public class RateLimiter {
    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final ConcurrentHashMap<Platform, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * 为平台注册一个令牌桶
     *
     * @param platform      平台
     * @param permitsPerSec 每秒产生的令牌数
     */
    public void register(Platform platform, double permitsPerSec) {
        buckets.put(platform, new TokenBucket(permitsPerSec));
        log.info("RateLimiter registered for {}: {} req/s", platform, permitsPerSec);
    }

    /**
     * 获取令牌，阻塞直到有可用令牌
     *
     * @param platform 平台
     * @throws InterruptedException 等待时被中断
     */
    public void acquire(Platform platform) throws InterruptedException {
        TokenBucket bucket = buckets.get(platform);
        if (bucket == null) {
            // 未注册的平台不限流
            log.warn("No rate limit configured for {}, skipping rate limiting", platform);
            return;
        }
        bucket.acquire();
    }

    /**
     * 尝试获取令牌（非阻塞）
     *
     * @param platform 平台
     * @return true如果获得令牌
     */
    public boolean tryAcquire(Platform platform) {
        TokenBucket bucket = buckets.get(platform);
        return bucket == null || bucket.tryAcquire();
    }

    // === 令牌桶内部类 ===

    private static class TokenBucket {
        private final double permitsPerSec;
        private final long maxBurstUs;  // 最大突发持续时间（微秒）
        private double tokens;
        private long lastRefillTime;    // 上次补充时间（纳秒）
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition tokensAvailable = lock.newCondition();

        TokenBucket(double permitsPerSec) {
            this.permitsPerSec = permitsPerSec;
            this.maxBurstUs = 1_000_000L;  // 1秒的突发容量
            this.tokens = permitsPerSec;    // 初始满令牌
            this.lastRefillTime = System.nanoTime();
        }

        void acquire() throws InterruptedException {
            lock.lock();
            try {
                refill();
                while (tokens < 1.0) {
                    // 计算需要等待的时间
                    double needed = 1.0 - tokens;
                    long waitNs = (long) (needed / permitsPerSec * 1_000_000_000L);
                    if (waitNs <= 0) waitNs = 1;
                    tokensAvailable.awaitNanos(waitNs);
                    refill();
                }
                tokens -= 1.0;
            } finally {
                lock.unlock();
            }
        }

        boolean tryAcquire() {
            lock.lock();
            try {
                refill();
                if (tokens >= 1.0) {
                    tokens -= 1.0;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsedNs = now - lastRefillTime;
            if (elapsedNs <= 0) return;

            double newTokens = (double) elapsedNs / 1_000_000_000L * permitsPerSec;
            tokens = Math.min(permitsPerSec, tokens + newTokens);
            lastRefillTime = now;

            if (tokens >= 1.0) {
                tokensAvailable.signalAll();
            }
        }
    }
}
