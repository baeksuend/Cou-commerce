package com.backsuend.coucommerce.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author rua
 */

@MappedSuperclass
public abstract class BaseTimeEntity {
    @Column(name = "createdAt", nullable = false)
    protected LocalDateTime createdAt;

    @Column(name = "updatedAt", nullable = false)
    protected LocalDateTime updatedAt;

    @Column(name = "deletedAt")
    protected LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}
