package com.venky.assistant.action;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pending_actions")
public class PendingAction {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "action_type", nullable = false, length = 60)
    private String actionType;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode payload;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(columnDefinition = "text")
    private String result;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PendingAction() {}

    public PendingAction(UUID userId, String actionType, JsonNode payload, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.actionType = actionType;
        this.payload = payload;
        this.status = "PENDING";
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getActionType() { return actionType; }
    public JsonNode getPayload() { return payload; }
    public String getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getExecutedAt() { return executedAt; }
    public String getResult() { return result; }
    public Instant getCreatedAt() { return createdAt; }

    public void approve() {
        this.status = "APPROVED";
        this.approvedAt = Instant.now();
    }

    public void reject() {
        this.status = "REJECTED";
    }

    public void markExecuted(String result) {
        this.status = "EXECUTED";
        this.executedAt = Instant.now();
        this.result = result;
    }

    public void markFailed(String error) {
        this.status = "FAILED";
        this.result = error;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }
}
