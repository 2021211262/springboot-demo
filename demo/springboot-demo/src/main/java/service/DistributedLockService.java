package com.example.springbootdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    @Autowired
    private StringRedisTemplate redisTemplate;

    public String tryLock(String key, long expireSeconds) {
        String value = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(success)) {
            log.debug("Distributed lock acquired: key={}, value={}", key, value);
            return value;
        }
        log.debug("Distributed lock acquisition failed: key={}", key);
        return null;
    }

    public String tryLockWithRetry(String key, long expireSeconds, int retryCount, long retryIntervalMs) {
        for (int i = 0; i <= retryCount; i++) {
            String lockValue = tryLock(key, expireSeconds);
            if (lockValue != null) {
                return lockValue;
            }
            if (i < retryCount) {
                try {
                    Thread.sleep(retryIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    public void unlock(String key, String value) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(key), value);
        if (result != null && result == 1L) {
            log.debug("Distributed lock released: key={}, value={}", key, value);
        } else {
            log.warn("Distributed lock release failed (lock may have expired): key={}, value={}", key, value);
        }
    }
}
