package com.example.concertreservation.global.cache;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Cache Stampede 방어 — 분산 락 (Redisson) 기반 Cache-Aside Pattern.
 *
 * 동작 원리:
 * 1. 캐시 확인 (fast path)
 * 2. 미스 시 Redisson 분산 락 획득
 * 3. 락 획득 후 재확인 (double-check) — 다른 스레드가 이미 채웠을 수 있음
 * 4. 여전히 미스 → DB 조회 → 캐시 저장
 *
 * sync=true 차이점: JVM 로컬 동기화 vs 분산 환경 전체 동기화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheStampedeGuard {

    private static final String LOCK_PREFIX = "cache:lock:";
    private static final long LOCK_WAIT_MS = 1_000L;
    private static final long LOCK_LEASE_MS = 5_000L;

    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;

    @SuppressWarnings("unchecked")
    public <T> T protect(String cacheName, String key, Supplier<T> loader) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return loader.get();
        }

        // 1. fast path: 캐시 히트 시 즉시 반환
        Cache.ValueWrapper cached = cache.get(key);
        if (cached != null) {
            return (T) cached.get();
        }

        // 2. 분산 락 획득 (멀티 인스턴스 환경에서도 단 1개 스레드만 DB 조회)
        RLock lock = redissonClient.getLock(LOCK_PREFIX + cacheName + ":" + key);
        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_MS, LOCK_LEASE_MS, TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("[CacheStampedeGuard] 락 타임아웃 — fallback to direct load. key={}", key);
                return loader.get();
            }

            // 3. double-check: 락 대기 중 다른 스레드가 캐시를 채웠을 수 있음
            cached = cache.get(key);
            if (cached != null) {
                return (T) cached.get();
            }

            // 4. 실제 DB 조회 + 캐시 저장
            T result = loader.get();
            cache.put(key, result);
            log.debug("[CacheStampedeGuard] 캐시 갱신 완료. key={}", key);
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[CacheStampedeGuard] 락 대기 중 인터럽트. key={}", key);
            return loader.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
