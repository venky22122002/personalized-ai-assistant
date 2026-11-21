package com.venky.assistant.action;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.venky.assistant.task.TaskService;
import java.time.OffsetDateTime;

@Service
public class ActionService {

	/** Action types the application will actually execute. No arbitrary code. */
	private static final List<String> ALLOWED_TYPES = List.of("CREATE_TASK", "COMPLETE_TASK", "UPDATE_TASK");

	private final ActionRepository actionRepo;
	private final TaskService taskService;
	private final ObjectMapper objectMapper;

	public ActionService(ActionRepository actionRepo, TaskService taskService, ObjectMapper objectMapper) {
		this.actionRepo = actionRepo;
		this.taskService = taskService;
		this.objectMapper = objectMapper;
	}

	/** Propose an action. AI calls this; user must approve before execution. */
	@Transactional
	public ActionResponse propose(UUID userId, String actionType, JsonNode payload) {

		if (!ALLOWED_TYPES.contains(actionType)) {
			throw new IllegalArgumentException("Unknown action type: " + actionType);
		}

		if (payload == null || !payload.isObject()) {
			throw new IllegalArgumentException("Action payload must be a JSON object");
		}

		// Reuse an identical pending proposal instead of creating a duplicate.
		List<PendingAction> pendingActions = actionRepo.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING");

		for (PendingAction existing : pendingActions) {
			if (actionType.equals(existing.getActionType()) && payload.equals(existing.getPayload())
					&& !existing.isExpired()) {

				return toResponse(existing);
			}
		}

		PendingAction action = new PendingAction(userId, actionType, payload, Instant.now().plus(24, ChronoUnit.HOURS));

		actionRepo.save(action);

		return toResponse(action);
	}

	/** Approve and execute the action — uses pessimistic lock. */
	@Transactional
	public ActionResponse approve(UUID userId, UUID actionId) {
		PendingAction action = actionRepo.findByIdAndUserIdForUpdate(actionId, userId)
				.orElseThrow(() -> new IllegalArgumentException("Action not found"));

		if (!action.isPending()) {
			throw new IllegalStateException("Action is already " + action.getStatus());
		}
		if (action.isExpired()) {
			action.reject();
			actionRepo.save(action);
			throw new IllegalStateException("Action has expired");
		}

		action.approve();

		String result = execute(action);
		action.markExecuted(result);

		return toResponse(actionRepo.save(action));
	}

	@Transactional
	public ActionResponse reject(UUID userId, UUID actionId) {
		PendingAction action = actionRepo.findByIdAndUserIdForUpdate(actionId, userId)
				.orElseThrow(() -> new IllegalArgumentException("Action not found"));

		if (!action.isPending()) {
			throw new IllegalStateException("Action is already " + action.getStatus());
		}
		action.reject();
		return toResponse(actionRepo.save(action));
	}

	@Transactional(readOnly = true)
	public List<ActionResponse> getPendingActions(UUID userId) {
		return actionRepo.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING").stream().map(this::toResponse)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<ActionResponse> getAllActions(UUID userId) {
		return actionRepo.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse)
				.collect(Collectors.toList());
	}

	// -----------------------------------------------------------------------

	private String execute(PendingAction action) {
		JsonNode p = action.getPayload();
		return switch (action.getActionType()) {

		case "CREATE_TASK" -> {
			String title = p.path("title").asText(null);

			if (title == null || title.isBlank()) {
				throw new IllegalArgumentException("Task title is required");
			}

			String priority = p.path("priority").asText("MEDIUM");
			String description = p.path("description").asText(null);

			java.time.Instant dueAt = null;

			if (p.hasNonNull("dueAt") && !p.path("dueAt").asText().isBlank()) {
				try {
					dueAt = OffsetDateTime.parse(p.path("dueAt").asText()).toInstant();
				} catch (java.time.format.DateTimeParseException e) {
					throw new IllegalArgumentException(
							"dueAt must be a valid ISO-8601 instant, for example " + "2026-10-20T03:30:00Z");
				}
			}

			var task = taskService.createTask(action.getUserId(), title, description, priority, dueAt);

			yield "Created task: " + task.title() + " [" + task.id() + "]" + (dueAt != null ? " due at " + dueAt : "");
		}

		case "COMPLETE_TASK" -> {
			UUID taskId = UUID.fromString(p.path("taskId").asText());
			taskService.markComplete(action.getUserId(), taskId);
			yield "Marked task complete: " + taskId;
		}
		case "UPDATE_TASK" -> {
			UUID taskId = UUID.fromString(p.path("taskId").asText());
			taskService.updateTask(action.getUserId(), taskId, p.has("title") ? p.path("title").asText(null) : null,
					p.has("description") ? p.path("description").asText(null) : null,
					p.has("status") ? p.path("status").asText(null) : null,
					p.has("priority") ? p.path("priority").asText(null) : null, null);
			yield "Updated task: " + taskId;
		}
		default -> throw new IllegalArgumentException("Unhandled action type: " + action.getActionType());
		};
	}

	private ActionResponse toResponse(PendingAction a) {
		return new ActionResponse(a.getId(), a.getActionType(), a.getPayload(), a.getStatus(), a.getExpiresAt(),
				a.getResult(), a.getCreatedAt());
	}

	public record ActionResponse(UUID id, String actionType, JsonNode payload, String status, Instant expiresAt,
			String result, Instant createdAt) {
	}
}
