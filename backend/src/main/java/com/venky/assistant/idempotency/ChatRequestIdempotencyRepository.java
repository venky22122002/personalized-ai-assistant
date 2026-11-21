
package com.venky.assistant.idempotency;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ChatRequestIdempotencyRepository
		extends JpaRepository<ChatRequestIdempotency, ChatRequestIdempotencyId> {

	Optional<ChatRequestIdempotency> findByUserIdAndRequestId(UUID userId, UUID requestId);

	@Query("""
			SELECT r
			FROM ChatRequestIdempotency r
			WHERE r.status = 'PROCESSING'
			  AND r.updatedAt < :cutoff
			""")
	List<ChatRequestIdempotency> findStaleProcessingRequests(@Param("cutoff") Instant cutoff);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT r
			FROM ChatRequestIdempotency r
			WHERE r.userId = :userId
			  AND r.requestId = :requestId
			""")
	Optional<ChatRequestIdempotency> findByUserIdAndRequestIdForUpdate(@Param("userId") UUID userId,
			@Param("requestId") UUID requestId);
}
