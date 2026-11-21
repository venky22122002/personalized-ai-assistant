package com.venky.assistant.memory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {

    List<Memory> findByUserIdOrderByImportanceDescCreatedAtDesc(UUID userId);

    List<Memory> findByUserIdAndCategoryOrderByImportanceDesc(UUID userId, String category);

    Optional<Memory> findByIdAndUserId(UUID id, UUID userId);

    /** PostgreSQL full-text search on memory content. */
    @Query(value = """
            SELECT * FROM memories
            WHERE user_id = :userId
              AND to_tsvector('english', content) @@ plainto_tsquery('english', :query)
            ORDER BY importance DESC, created_at DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Memory> searchByText(@Param("userId") UUID userId, @Param("query") String query);
}
