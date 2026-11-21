
package com.venky.assistant.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /** Most recent 20 messages, returned newest-first for slicing. */
    List<ChatMessage> findTop20ByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    /** All messages in chronological order for display. */
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
