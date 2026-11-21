
package com.venky.assistant.context;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record AssistantContext(

		ContextIntent intent,

		String timezone,

		ZonedDateTime currentDateTime,

		List<MemoryContext> memories,

		List<TaskContext> tasks,

		List<ReminderContext> reminders,

		List<MessageContext> conversationHistory

) {

	public AssistantContext {
		memories = memories == null ? List.of() : List.copyOf(memories);
		tasks = tasks == null ? List.of() : List.copyOf(tasks);
		reminders = reminders == null ? List.of() : List.copyOf(reminders);
		conversationHistory = conversationHistory == null ? List.of() : List.copyOf(conversationHistory);
	}

	public record MemoryContext(UUID id, String category, String content, String source, short importance,
			double similarity) {
	}

	public record TaskContext(UUID id, String title, String description, String status, String priority, Instant dueAt,
			Instant createdAt) {
	}

	public record ReminderContext(UUID id, UUID taskId, String title, Instant remindAt, String status) {
	}

	public record MessageContext(String role, String content, Instant createdAt) {
	}
}
