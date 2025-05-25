package com.kvstore.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core storage engine that handles data persistence and retrieval
 */
public class StorageEngine {
    private static final Logger logger = LoggerFactory.getLogger(StorageEngine.class);

    private final Map<String, StorageEntry> store;
    private final ReadWriteLock lock;

    public StorageEngine() {
        this.store = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public void set(String key, Object value, DataType type) {
        lock.writeLock().lock();
        try {
            store.put(key, new StorageEntry(value, type));
            logger.debug("Set key: {} with type: {}", key, type);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<StorageEntry> get(String key) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(store.get(key));
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean delete(String key) {
        lock.writeLock().lock();
        try {
            return store.remove(key) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean exists(String key) {
        lock.readLock().lock();
        try {
            return store.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public long size() {
        lock.readLock().lock();
        try {
            return store.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            store.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}