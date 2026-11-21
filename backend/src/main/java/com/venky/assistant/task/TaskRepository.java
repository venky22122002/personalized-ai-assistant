package com.venky.assistant.task;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
	List<Task> findByUserIdAndStatusNotOrderByPriorityDescCreatedAtDesc(UUID userId, String status);

	List<Task> findByUserIdOrderByCreatedAtDesc(UUID userId);

	Optional<Task> findByIdAndUserId(UUID id, UUID userId);

	List<Task> findByUserIdAndStatusInOrderByPriorityDescCreatedAtAsc(UUID userId, List<String> statuses);

}
