package com.venky.assistant.action;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActionRepository extends JpaRepository<PendingAction, UUID> {

    List<PendingAction> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    List<PendingAction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Pessimistic lock to prevent double-approval. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM PendingAction a WHERE a.id = :id AND a.userId = :userId")
    Optional<PendingAction> findByIdAndUserIdForUpdate(
            @Param("id") UUID id, @Param("userId") UUID userId);
}
