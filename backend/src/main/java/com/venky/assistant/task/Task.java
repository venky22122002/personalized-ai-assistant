package com.venky.assistant.task;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 10)
    private String priority;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {}

    public Task(UUID userId, String title, String description,
                String priority, Instant dueAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.status = "TODO";
        this.priority = priority != null ? priority.toUpperCase() : "MEDIUM";
        this.dueAt = dueAt;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public Instant getDueAt() { return dueAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; this.updatedAt = Instant.now(); }
    public void setDescription(String description) { this.description = description; this.updatedAt = Instant.now(); }
    public void setStatus(String status) { this.status = status; this.updatedAt = Instant.now(); }
    public void setPriority(String priority) { this.priority = priority.toUpperCase(); this.updatedAt = Instant.now(); }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; this.updatedAt = Instant.now(); }
}
