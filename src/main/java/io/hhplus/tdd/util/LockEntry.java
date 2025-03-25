package io.hhplus.tdd.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class LockEntry {

    private final ReentrantLock lock;
    private final AtomicInteger usageCount = new AtomicInteger(1);

    public LockEntry(ReentrantLock lock) {
        this.lock = lock;
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
        usageCount.decrementAndGet();
    }

    public void incrementUsage() {
        usageCount.incrementAndGet();
    }

    public int getUsageCount() {
        return usageCount.get();
    }

}
