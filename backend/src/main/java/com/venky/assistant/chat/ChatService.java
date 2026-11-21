package com.venky.assistant.chat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.venky.assistant.action.ActionService;
import com.venky.assistant.idempotency.ChatRequestIdempotencyService;
import com.venky.assistant.memory.MemoryExtractionService;
import com.venky.assistant.memory.MemoryService;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

@Service
public class ChatService {

	private static final Logger log = LoggerFactory.getLogger(ChatService.class);

	private final AssistantAi assistantAi;
	private final ConversationRepository conversationRepo;
	private final ChatMessageRepository messageRepo;
	private final MemoryService memoryService;
	private final MemoryExtractionService memoryExtractionService;
	private final ActionIntentAi actionIntentAi;
	private final ActionService actionService;
	private final ObjectMapper objectMapper;
	private final ChatRequestIdempotencyService idempotencyService;

	public ChatService(ChatModel chatModel, ConversationRepository conversationRepo, ChatMessageRepository messageRepo,
			MemoryService memoryService, MemoryExtractionService memoryExtractionService, ActionService actionService,
			ObjectMapper objectMapper, ChatRequestIdempotencyService idempotencyService) {

		this.assistantAi = AiServices.builder(AssistantAi.class).chatModel(chatModel).build();

		this.actionIntentAi = AiServices.builder(ActionIntentAi.class).chatModel(chatModel).build();

		this.conversationRepo = conversationRepo;
		this.messageRepo = messageRepo;
		this.memoryService = memoryService;
		this.memoryExtractionService = memoryExtractionService;
		this.actionService = actionService;
		this.objectMapper = objectMapper;
		this.idempotencyService = idempotencyService;
	}

	/** Send a message and return the reply. Creates conversation if needed. */
	@Transactional
	public ChatResponse chat(UUID userId, UUID conversationId, String userMessage, UUID requestId) {
		if (userMessage == null || userMessage.isBlank()) {
			throw new IllegalArgumentException("Message cannot be empty");
		}
		if (userMessage.length() > 8000) {
			throw new IllegalArgumentException("Message is too long (max 8000 chars)");
		}
		idempotencyService.lockProcessingRequest(userId, requestId);

		// Resolve or create conversation
		Conversation conversation;
		if (conversationId != null) {
			conversation = conversationRepo.findByIdAndUserId(conversationId, userId)
					.orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
		} else {
			conversation = conversationRepo.save(new Conversation(userId, deriveTitle(userMessage)));
		}

		// Build context: recent history + relevant memories
		String contextualMessage = buildContextualMessage(userId, conversation.getId(), userMessage);

		// Persist user message
		messageRepo.save(new ChatMessage(conversation.getId(), "USER", userMessage));

		// Call the AI
		// Detect a requested action and create a pending proposal.
		ActionService.ActionResponse proposedAction = detectAndProposeAction(userId, userMessage);

		String reply;
		List<String> proposedActions = List.of();

		if (proposedAction != null) {
			reply = "I've prepared a " + proposedAction.actionType() + " proposal for your review. "
					+ "No task changes have been executed yet. "
					+ "Review the pending action and approve it if you want " + "me to proceed. Proposal ID: "
					+ proposedAction.id();

			proposedActions = List.of(proposedAction.id().toString());
		} else {
			// Ordinary chat continues through the existing assistant.
			reply = assistantAi.chat(contextualMessage);
		}

		// Persist assistant reply.
		messageRepo.save(new ChatMessage(conversation.getId(), "ASSISTANT", reply));

		// Extract useful memories from the original user message.
		// The extraction service handles its own failures.
		memoryExtractionService.extractAndSave(userId, userMessage);

		ChatResponse response = new ChatResponse(conversation.getId(), reply, proposedActions);

		JsonNode responseJson = objectMapper.valueToTree(response);

		idempotencyService.complete(userId, requestId, responseJson);

		return response;
	}

	private ActionService.ActionResponse detectAndProposeAction(UUID userId, String userMessage) {

		try {
			ZoneId userZone = ZoneId.of("Asia/Kolkata");
			ZonedDateTime now = ZonedDateTime.now(userZone);

			String classifierInput = """
					Current date and time: %s
					User timezone: Asia/Kolkata.

					Interpret relative dates using the current date and time above.
					For example, "tomorrow" means the next calendar day
					in Asia/Kolkata.

					IMPORTANT DATETIME RULES:
					- Return dueAtLocal as the requested India local date and time.
					- Format: yyyy-MM-dd'T'HH:mm:ss
					- Do not convert to UTC.
					- Do not append Z or any timezone offset.
					- If no deadline is specified, use null.
					- Return only the JSON object required by your system instructions.

					User request:
					%s
					""".formatted(now, userMessage);

			String json = actionIntentAi.classify(classifierInput);

			// Remove optional Markdown JSON code fences.
			json = json.trim();

			if (json.startsWith("```")) {
				json = json.replaceFirst("^```(?:json)?\\s*", "");
				json = json.replaceFirst("\\s*```\\s*$", "");
			}

			log.info("Action classifier JSON after cleanup: {}", json);

			JsonNode result = objectMapper.readTree(json);

			JsonNode typeNode = result.get("actionType");
			JsonNode payload = result.get("payload");

			if (typeNode == null || !typeNode.isTextual() || payload == null || !payload.isObject()) {
				return null;
			}

			String actionType = typeNode.asText().trim().toUpperCase();

			if ("NONE".equals(actionType)) {
				return null;
			}

			// Validate required fields before creating a proposal.
			if ("CREATE_TASK".equals(actionType)) {

				ObjectNode taskPayload = (ObjectNode) payload;
				// Keep only fields supported by CREATE_TASK.
				taskPayload.retain("title", "description", "priority", "dueAtLocal");

				// Validate task title.
				JsonNode titleNode = taskPayload.get("title");

				if (titleNode == null || !titleNode.isTextual() || titleNode.asText().isBlank()) {
					log.warn("Rejected CREATE_TASK: missing title");
					return null;
				}

				// Validate priority.
				String priority = taskPayload.path("priority").asText("MEDIUM").toUpperCase();

				if (!List.of("LOW", "MEDIUM", "HIGH").contains(priority)) {
					log.warn("Rejected CREATE_TASK: invalid priority {}", priority);
					return null;
				}

				taskPayload.put("priority", priority);

				// Convert the requested India local time to a UTC instant.
				JsonNode localNode = taskPayload.get("dueAtLocal");

				if (localNode != null && !localNode.isNull() && !localNode.asText().isBlank()) {

					try {
						String localText = localNode.asText().trim();

						// Compatibility with the model incorrectly appending Z
						// to a field explicitly defined as local time.
						if (localText.endsWith("Z")) {
							log.warn("dueAtLocal unexpectedly ends in Z; "
									+ "interpreting its clock time as Asia/Kolkata local time");
							localText = localText.substring(0, localText.length() - 1);
						}

						LocalDateTime localDateTime = LocalDateTime.parse(localText);

						Instant dueAt = localDateTime.atZone(ZoneId.of("Asia/Kolkata")).toInstant();

						if (!dueAt.isAfter(Instant.now())) {
							log.warn("Rejected task proposal with past deadline: {}", dueAt);
							return null;
						}

						taskPayload.put("dueAt", dueAt.toString());

					} catch (java.time.format.DateTimeParseException e) {
						log.warn("Rejected task proposal: invalid dueAtLocal {}", localNode.asText(), e);
						return null;
					}
				}

				// Remove classifier-only fields before persisting the proposal.
				taskPayload.remove("dueAtLocal");
				taskPayload.remove("completeTask");

			} else if ("UPDATE_TASK".equals(actionType) || "COMPLETE_TASK".equals(actionType)) {

				JsonNode taskId = payload.get("taskId");

				if (taskId == null || !taskId.isTextual()) {
					log.warn("Rejected {}: missing taskId", actionType);
					return null;
				}

				try {
					UUID.fromString(taskId.asText());
				} catch (IllegalArgumentException e) {
					log.warn("Rejected {}: invalid taskId", actionType);
					return null;
				}

			} else {
				log.warn("Rejected unsupported action type: {}", actionType);
				return null;
			}

			// Persist a pending proposal. The task is NOT executed here.
			ActionService.ActionResponse response = actionService.propose(userId, actionType, payload);

			log.info("Created pending action proposal: id={}, type={}, status={}", response.id(), response.actionType(),
					response.status());

			return response;

		} catch (Exception e) {
			log.warn("Action classification or proposal failed: {}", e.getMessage(), e);
			return null;
		}
	}

	/** Retrieve all messages in a conversation (owner-scoped). */
	@Transactional(readOnly = true)
	public List<MessageResponse> getMessages(UUID userId, UUID conversationId) {
		conversationRepo.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

		return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
				.map(m -> new MessageResponse(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
				.collect(Collectors.toList());
	}

	/** List all conversations for a user. */
	@Transactional(readOnly = true)
	public List<ConversationSummary> listConversations(UUID userId) {
		return conversationRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
				.map(c -> new ConversationSummary(c.getId(), c.getTitle(), c.getCreatedAt()))
				.collect(Collectors.toList());
	}

	// -----------------------------------------------------------------------

	private String buildContextualMessage(UUID userId, UUID convId, String userMessage) {

		List<ChatMessage> history = messageRepo.findTop20ByConversationIdOrderByCreatedAtDesc(convId);

		Collections.reverse(history);

		boolean genericIntroduction = isGenericIntroduction(userMessage);

		List<String> memories = genericIntroduction ? Collections.emptyList()
				: memoryService.searchRelevantMemories(userId, userMessage);

		StringBuilder sb = new StringBuilder();

		sb.append("""
				
				Answer the current user's message naturally.

		        The following context is internal background information.
		        Do not describe how it was retrieved, stored, or assembled
		        unless the user explicitly asks.

		        IMPORTANT MEMORY RULES:
				1. Relevant memories represent the user's latest known facts
				   and preferences.
				2. If a relevant memory conflicts with older conversation
				   messages, prefer the relevant memory.
				3. Treat older conversation messages as historical context,
				   not necessarily as the user's current preferences.
				4. If the user explicitly changes a preference in the current
				   message, acknowledge the new preference.
				5. Never state a superseded preference as if it were current.
				6. Do not invent tasks or actions the user did not request.
				7. Answer the user's current message directly and naturally.

				""");

		if (!memories.isEmpty()) {
			sb.append("[CURRENT RELEVANT MEMORIES]\n");

			memories.forEach(memory -> sb.append("- ").append(memory).append("\n"));

			sb.append("""

					These memories are contextual information about the
					currently authenticated user.

					Use them only when relevant to the user's current request.

					Do not introduce unrelated personal interests, technologies,
					projects, occupations, or preferences into your response.

					For general questions about your identity or capabilities,
					provide a general-purpose introduction.

					Never assume all users share the same interests.

					""");
		}

		if (!history.isEmpty()) {
			sb.append("[PREVIOUS CONVERSATION — HISTORICAL CONTEXT]\n");

			history.forEach(
					message -> sb.append(message.getRole()).append(": ").append(message.getContent()).append("\n"));

			sb.append("\n");
		}

		sb.append("[CURRENT USER MESSAGE]\n").append(userMessage);

		return sb.toString();
	}

	private boolean isGenericIntroduction(String message) {

		if (message == null) {
			return false;
		}

		String normalized = message.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ")
				.replaceAll("\\s+", " ").trim();

		return normalized.equals("who are you") || normalized.equals("introduce yourself")
				|| normalized.equals("what is your name") || normalized.equals("tell me about yourself")
				|| normalized.equals("what can you do") || normalized.equals("what are your capabilities")
				|| normalized.equals("who are you what is your name and who created you");
	}

	private String deriveTitle(String message) {
		String title = message.length() > 60 ? message.substring(0, 57) + "…" : message;
		return title.strip();
	}

	// DTO records
	public record ChatResponse(UUID conversationId, String reply, List<String> proposedActions) {
	}

	public record MessageResponse(UUID id, String role, String content, java.time.Instant createdAt) {
	}

	public record ConversationSummary(UUID id, String title, java.time.Instant createdAt) {
	}
}
