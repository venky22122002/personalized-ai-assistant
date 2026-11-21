package com.venky.assistant.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskService {

	private static final List<String> VALID_STATUSES = List.of("TODO", "IN_PROGRESS", "DONE", "CANCELLED");
	private static final List<String> VALID_PRIORITIES = List.of("LOW", "MEDIUM", "HIGH");

	private final TaskRepository taskRepo;

	public TaskService(TaskRepository taskRepo) {
		this.taskRepo = taskRepo;
	}

	@Transactional
	public TaskResponse createTask(UUID userId, String title, String description, String priority, Instant dueAt) {
		if (title == null || title.isBlank())
			throw new IllegalArgumentException("Title is required");
		if (priority != null && !VALID_PRIORITIES.contains(priority.toUpperCase()))
			throw new IllegalArgumentException("Invalid priority");

		Task task = new Task(userId, title.strip(), description, priority, dueAt);
		taskRepo.save(task);
		return toResponse(task);
	}

	@Transactional(readOnly = true)
	public List<TaskResponse> getTasks(UUID userId, boolean openOnly) {
		List<Task> tasks = openOnly
				? taskRepo.findByUserIdAndStatusNotOrderByPriorityDescCreatedAtDesc(userId, "CANCELLED")
				: taskRepo.findByUserIdOrderByCreatedAtDesc(userId);
		return tasks.stream().map(this::toResponse).collect(Collectors.toList());
	}

	@Transactional
	public TaskResponse updateTask(UUID userId, UUID taskId, String title, String description, String status,
			String priority, Instant dueAt) {
		Task task = taskRepo.findByIdAndUserId(taskId, userId)
				.orElseThrow(() -> new IllegalArgumentException("Task not found"));

		if (title != null && !title.isBlank())
			task.setTitle(title.strip());
		if (description != null)
			task.setDescription(description);
		if (status != null) {
			if (!VALID_STATUSES.contains(status.toUpperCase()))
				throw new IllegalArgumentException("Invalid status");
			task.setStatus(status.toUpperCase());
		}
		if (priority != null) {
			if (!VALID_PRIORITIES.contains(priority.toUpperCase()))
				throw new IllegalArgumentException("Invalid priority");
			task.setPriority(priority.toUpperCase());
		}
		if (dueAt != null)
			task.setDueAt(dueAt);

		return toResponse(taskRepo.save(task));
	}

	@Transactional
	public void markComplete(UUID userId, UUID taskId) {
		Task task = taskRepo.findByIdAndUserId(taskId, userId)
				.orElseThrow(() -> new IllegalArgumentException("Task not found"));
		task.setStatus("DONE");
		taskRepo.save(task);
	}

	private TaskResponse toResponse(Task t) {
		return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(),
				t.getDueAt(), t.getCreatedAt(), t.getUpdatedAt());
	}

	public record TaskResponse(UUID id, String title, String description, String status, String priority, Instant dueAt,
			Instant createdAt, Instant updatedAt) {
	}

	@Transactional(readOnly = true)
	public List<TaskResponse> getActiveTasksForContext(UUID userId) {
		List<Task> tasks = taskRepo.findByUserIdAndStatusInOrderByPriorityDescCreatedAtAsc(userId,
				List.of("TODO", "IN_PROGRESS"));

		return tasks.stream().map(this::toResponse).collect(Collectors.toList());
	}

}
