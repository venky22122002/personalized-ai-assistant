
package com.venky.assistant.reminder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReminderService {

	private final ReminderRepository reminderRepository;

	public ReminderService(ReminderRepository reminderRepository) {
		this.reminderRepository = reminderRepository;
	}

	@Transactional(readOnly = true)
	public List<ReminderResponse> getScheduledRemindersForContext(UUID userId, Instant start, Instant end) {

		if (userId == null) {
			throw new IllegalArgumentException("User ID is required");
		}

		if (start == null || end == null || end.isBefore(start)) {
			throw new IllegalArgumentException("Invalid reminder time range");
		}

		return reminderRepository
				.findByUserIdAndStatusAndRemindAtBetweenOrderByRemindAtAsc(userId, "SCHEDULED", start, end).stream()
				.map(this::toResponse).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<ReminderResponse> getAllScheduledReminders(UUID userId) {
		if (userId == null) {
			throw new IllegalArgumentException("User ID is required");
		}

		return reminderRepository.findByUserIdAndStatusOrderByRemindAtAsc(userId, "SCHEDULED").stream()
				.map(this::toResponse).collect(Collectors.toList());
	}

	private ReminderResponse toResponse(Reminder reminder) {
		return new ReminderResponse(reminder.getId(), reminder.getTaskId(), reminder.getTitle(), reminder.getRemindAt(),
				reminder.getStatus());
	}

	public record ReminderResponse(UUID id, UUID taskId, String title, Instant remindAt, String status) {
	}
}
