
package com.venky.assistant.memory;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@Service
public class MemoryExtractionService {

	private static final Logger log = LoggerFactory.getLogger(MemoryExtractionService.class);

	private final MemoryExtractionAi extractor;
	private final MemoryService memoryService;
	private final ObjectMapper objectMapper;

	public MemoryExtractionService(ChatModel chatModel, MemoryService memoryService, ObjectMapper objectMapper) {

		this.extractor = AiServices.builder(MemoryExtractionAi.class).chatModel(chatModel).build();

		this.memoryService = memoryService;
		this.objectMapper = objectMapper;
	}

	public void extractAndSave(UUID userId, String userMessage) {
		try {
			List<MemoryService.MemoryResponse> existing = memoryService.getAllMemories(userId, null);

			StringBuilder input = new StringBuilder();

			input.append("LATEST USER MESSAGE:\n").append(userMessage).append("\n\nEXISTING MEMORIES:\n");

			for (MemoryService.MemoryResponse memory : existing) {
				input.append("ID: ").append(memory.id()).append(" | CATEGORY: ").append(memory.category())
						.append(" | CONTENT: ").append(memory.content()).append("\n");
			}

			if (existing.isEmpty()) {
				input.append("No existing memories.\n");
			}

			String output = extractor.extractMemories(input.toString());

			if (output == null || output.isBlank()) {
				return;
			}

			String json = output.trim();

			if (json.startsWith("```")) {
				json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
			}

			List<MemoryAction> actions = objectMapper.readValue(json, new TypeReference<List<MemoryAction>>() {
			});

			Set<String> knownContents = new HashSet<>();

			existing.forEach(memory -> knownContents.add(normalize(memory.content())));

			int processed = 0;

			for (MemoryAction action : actions) {
				if (processed >= 3) {
					break;
				}

				if (action == null || action.action() == null || action.content() == null
						|| action.content().isBlank()) {
					continue;
				}

				String operation = action.action().trim().toUpperCase(Locale.ROOT);

				String content = action.content().trim();
				String normalized = normalize(content);

				if (content.length() > 4000 || normalized.isBlank()) {
					continue;
				}

				if ("CREATE".equals(operation)) {
					if (knownContents.contains(normalized)) {
						continue;
					}

					if (action.category() == null) {
						continue;
					}

					short importance = (short) Math.max(1, Math.min(5, action.importance()));

					try {
						memoryService.saveMemory(userId, action.category().trim().toUpperCase(Locale.ROOT), content,
								"CHAT", importance);

						knownContents.add(normalized);
						processed++;
					} catch (RuntimeException e) {
						log.warn("Could not create memory: {}", e.getMessage());
					}

				} else if ("UPDATE".equals(operation)) {
					if (action.existingMemoryId() == null) {
						continue;
					}

					try {
						UUID memoryId = UUID.fromString(action.existingMemoryId());

						MemoryService.MemoryResponse target = existing.stream().filter(m -> m.id().equals(memoryId))
								.findFirst().orElse(null);

						// Only update a memory that belongs to this user
						// and was included in the supplied memory list.
						if (target == null) {
							continue;
						}

						// An unchanged fact does not need an update.
						if (normalize(target.content()).equals(normalized)) {
							continue;
						}

						memoryService.updateMemory(userId, memoryId, content);

						knownContents.remove(normalize(target.content()));
						knownContents.add(normalized);
						processed++;

					} catch (IllegalArgumentException e) {
						log.warn("Invalid memory update request: {}", e.getMessage());
					} catch (RuntimeException e) {
						log.warn("Could not update memory: {}", e.getMessage());
					}
				}
			}

		} catch (Exception e) {
			// Extraction must not prevent the normal chat response.
			log.warn("Automatic memory extraction failed: {}", e.getMessage());
		}
	}

	private String normalize(String content) {
		return content == null ? "" : content.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	private record MemoryAction(String action, String existingMemoryId, String category, String content,
			int importance) {
	}

	public interface MemoryExtractionAi {

		@SystemMessage("""
				You manage a user's long-term personal memories.

				The input contains a latest user message and existing
				memories, each with an ID, category, and content.

				Return ONLY a valid JSON array, without Markdown.

				Each item must contain:
				- action: CREATE or UPDATE
				- existingMemoryId: existing memory ID for UPDATE,
				  or null for CREATE
				- category: PREFERENCE, GOAL, FACT, SKILL, PROJECT,
				  HABIT, NOTE, or OTHER
				- content: concise, self-contained memory
				- importance: integer from 1 to 5

				Rules:
				1. Return at most 3 actions.
				2. CREATE only durable, useful information about the user,
					their preferences, goals, skills, habits, or ongoing projects.
					Never create memories simply to record a question.
				3. UPDATE only when the user explicitly corrects,
				   changes, or replaces an existing fact or preference.
				4. For UPDATE, use the exact ID of the memory being
				   replaced. Do not invent IDs.
				5. Do not update a memory merely because the new message
				   discusses the same topic.
				6. If a message changes a preference, preserve the new
				   preference as the current truth, not the old value.
				7. Do not save ordinary questions, general knowledge,
				   hypothetical examples, or temporary requests.
				8. Never extract passwords, tokens, or secrets.
				9. If nothing should be created or updated, return [].
				10. Do not output DELETE actions.
				
				11. Do not save a question merely because the user asked it.
				12. Do not save general knowledge questions, definitions,
					comparisons, coding questions, or troubleshooting requests
					as personal memories.
				13. A question should only produce a memory if it explicitly
					reveals useful personal information, such as:
					"I am learning Python for my AI project."
				14. Before creating a memory, ask:
					"Does this tell me something useful about the user,
					rather than simply what they asked?"
					If not, return no action.
				15. Do not create a NOTE containing the user's question
					just to preserve the conversation.

				Example:
				Existing memory:
				ID: abc-123 | CATEGORY: PREFERENCE |
				CONTENT: I prefer morning workouts.

				Latest user message:
				I changed my routine and now work out in the evening.

				Expected action:
				[{"action":"UPDATE","existingMemoryId":"abc-123",
				  "category":"PREFERENCE",
				  "content":"I prefer evening workouts.",
				  "importance":4}]

				The example ID is illustrative. Use only actual IDs
				supplied in the input.
				""")
		String extractMemories(@UserMessage String input);
	}
}
