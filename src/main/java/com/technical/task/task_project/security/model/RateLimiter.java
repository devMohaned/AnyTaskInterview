package com.technical.task.task_project.security.model;

import java.time.Duration;
import java.util.concurrent.Semaphore;

public class RateLimiter {
    private final Semaphore semaphore;
    private final int maxRequests;
    private long nextRefillTime;
    private final Duration refillDuration;

    public RateLimiter(int maxRequests, Duration refillDuration) {
        this.maxRequests = maxRequests;
        this.refillDuration = refillDuration;
        this.semaphore = new Semaphore(maxRequests);
        this.nextRefillTime = System.currentTimeMillis() + refillDuration.toMillis();
    }

    public synchronized boolean tryConsume() {
        refillTokensIfNecessary();
        return semaphore.tryAcquire();
    }

    private void refillTokensIfNecessary() {
        long now = System.currentTimeMillis();
        if (now > nextRefillTime) {
            semaphore.release(maxRequests - semaphore.availablePermits());
            nextRefillTime = now + refillDuration.toMillis();
        }
    }
}