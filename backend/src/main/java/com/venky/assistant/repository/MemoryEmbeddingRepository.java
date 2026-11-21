
package com.venky.assistant.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryEmbeddingRepository {

	private final JdbcTemplate jdbcTemplate;

	public MemoryEmbeddingRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void save(UUID memoryId, float[] vector) {

		String vectorText = toVectorLiteral(vector);

		jdbcTemplate.update("""
				INSERT INTO memory_embeddings
				    (memory_id, embedding, created_at, updated_at)
				VALUES (?, CAST(? AS vector), now(), now())
				ON CONFLICT (memory_id)
				DO UPDATE SET
				    embedding = EXCLUDED.embedding,
				    updated_at = now()
				""", memoryId, vectorText);
	}

	public List<MemorySearchResult> searchSimilar(UUID userId, float[] queryVector, int limit,
			double minimumSimilarity) {

		String vectorText = toVectorLiteral(queryVector);

		return jdbcTemplate.query("""
				SELECT
				    m.id,
				    m.category,
				    m.content,
				    m.source,
				    m.importance,
				    1 - (me.embedding <=> CAST(? AS vector)) AS similarity
				FROM memories m
				JOIN memory_embeddings me
				    ON me.memory_id = m.id
				WHERE m.user_id = ?
				  AND 1 - (me.embedding <=> CAST(? AS vector)) >= ?
				ORDER BY me.embedding <=> CAST(? AS vector)
				LIMIT ?
				""", (rs, rowNum) -> new MemorySearchResult(rs.getObject("id", UUID.class), rs.getString("category"),
				rs.getString("content"), rs.getString("source"), rs.getShort("importance"), rs.getDouble("similarity")),
				vectorText, userId, vectorText, minimumSimilarity, vectorText, limit);
	}

	private String toVectorLiteral(float[] vector) {

		StringBuilder result = new StringBuilder("[");

		for (int i = 0; i < vector.length; i++) {
			if (i > 0) {
				result.append(',');
			}

			result.append(vector[i]);
		}

		return result.append(']').toString();
	}

	public record MemorySearchResult(UUID id, String category, String content, String source, short importance,
			double similarity) {
	}
}
