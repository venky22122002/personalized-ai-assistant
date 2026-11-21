
package com.venky.assistant.reminder;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reminders")
public class Reminder {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "task_id")
	private UUID taskId;

	@Column(nullable = false, length = 250)
	private String title;

	@Column(name = "remind_at", nullable = false)
	private Instant remindAt;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Reminder() {
	}

	public Reminder(UUID userId, UUID taskId, String title, Instant remindAt) {
		this.id = UUID.randomUUID();
		this.userId = userId;
		this.taskId = taskId;
		this.title = title;
		this.remindAt = remindAt;
		this.status = "SCHEDULED";
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getTaskId() {
		return taskId;
	}

	public String getTitle() {
		return title;
	}

	public Instant getRemindAt() {
		return remindAt;
	}

	public String getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setRemindAt(Instant remindAt) {
		this.remindAt = remindAt;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
