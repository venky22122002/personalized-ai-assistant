
package com.venky.assistant.reminder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

	Optional<Reminder> findByIdAndUserId(UUID id, UUID userId);

	List<Reminder> findByUserIdAndStatusOrderByRemindAtAsc(UUID userId, String status);

	List<Reminder> findByUserIdAndStatusAndRemindAtBetweenOrderByRemindAtAsc(UUID userId, String status,
			java.time.Instant start, java.time.Instant end);
}
