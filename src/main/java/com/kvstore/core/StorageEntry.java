package com.kvstore.core;

import java.time.Instant;

/**
 * Represents a value stored in the key-value store
 */
public class StorageEntry {
    private final Object value;
    private final DataType type;
    private final Instant createdAt;
    private Instant expiresAt;

    public StorageEntry(Object value, DataType type) {
        this.value = value;
        this.type = type;
        this.createdAt = Instant.now();
    }

    public Object getValue() {
        return value;
    }

    public DataType getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}