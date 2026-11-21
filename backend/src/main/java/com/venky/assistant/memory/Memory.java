package com.venky.assistant.memory;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memories")
public class Memory {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(nullable = false)
    private short importance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Memory() {}

    public Memory(UUID userId, String category, String content,
                  String source, short importance) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.category = category;
        this.content = content;
        this.source = source;
        this.importance = importance;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getCategory() { return category; }
    public String getContent() { return content; }
    public String getSource() { return source; }
    public short getImportance() { return importance; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = Instant.now();
    }
}
