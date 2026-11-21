package com.venky.assistant.chat;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessage() {}

    public ChatMessage(UUID conversationId, String role, String content) {
        this.id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
