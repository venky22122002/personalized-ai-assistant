package com.venky.assistant.idempotency;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.JsonNode;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_request_idempotency")
@IdClass(ChatRequestIdempotencyId.class)
public class ChatRequestIdempotency {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(nullable = false, length = 20)
    private String status = "PROCESSING";

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode response;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChatRequestIdempotency() {
    }

    public ChatRequestIdempotency(UUID userId, UUID requestId, String requestHash) {
        this.userId = userId;
        this.requestId = requestId;
        this.requestHash = requestHash;
        this.status = "PROCESSING";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public JsonNode getResponse() {
        return response;
    }

    public void markCompleted(JsonNode response) {
        this.response = response;
        this.status = "COMPLETED";
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = "FAILED";
        this.updatedAt = Instant.now();
    }
}
