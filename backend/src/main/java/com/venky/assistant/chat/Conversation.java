package com.venky.assistant.chat;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = 200)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Conversation() {}

    public Conversation(UUID userId, String title) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.title = title;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }

    public void setTitle(String title) { this.title = title; }
}
