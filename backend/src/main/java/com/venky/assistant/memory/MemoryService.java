
package com.venky.assistant.memory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.venky.assistant.repository.MemoryEmbeddingRepository;
import com.venky.assistant.repository.MemoryEmbeddingRepository.MemorySearchResult;

import dev.langchain4j.model.embedding.EmbeddingModel;

@Service
public class MemoryService {

	private static final List<String> ALLOWED_CATEGORIES = List.of("PREFERENCE", "GOAL", "FACT", "SKILL", "PROJECT",
			"HABIT", "NOTE", "OTHER");

	private static final int DEFAULT_RESULT_LIMIT = 5;

	// Initial value; we'll tune this using actual retrieval tests.
	private static final double MINIMUM_SIMILARITY = 0.65;

	private final MemoryRepository memoryRepo;
	private final MemoryEmbeddingRepository embeddingRepository;
	private final EmbeddingModel embeddingModel;

	public MemoryService(MemoryRepository memoryRepo, MemoryEmbeddingRepository embeddingRepository,
			EmbeddingModel embeddingModel) {

		this.memoryRepo = memoryRepo;
		this.embeddingRepository = embeddingRepository;
		this.embeddingModel = embeddingModel;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public MemoryResponse saveMemory(UUID userId, String category, String content, String source, short importance) {

		validateCategory(category);

		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("Content is required");
		}

		if (content.length() > 4000) {
			throw new IllegalArgumentException("Content too long (max 4000)");
		}

		Memory memory = new Memory(userId, category.toUpperCase(), content, source, importance);

		Memory savedMemory = memoryRepo.saveAndFlush(memory);

		float[] vector = embeddingModel.embed(savedMemory.getContent()).content().vector();

		validateVector(vector);

		embeddingRepository.save(savedMemory.getId(), vector);

		return toResponse(savedMemory);
	}

	@Transactional(readOnly = true)
	public List<MemoryResponse> getAllMemories(UUID userId, String category) {

		if (category != null && !category.isBlank()) {
			return memoryRepo.findByUserIdAndCategoryOrderByImportanceDesc(userId, category.toUpperCase()).stream()
					.map(this::toResponse).collect(Collectors.toList());
		}

		return memoryRepo.findByUserIdOrderByImportanceDescCreatedAtDesc(userId).stream().map(this::toResponse)
				.collect(Collectors.toList());
	}

	/**
	 * Structured semantic retrieval for ContextService.
	 */
	@Transactional(readOnly = true)
	public List<MemorySearchResult> searchRelevantMemoryResults(UUID userId, String query) {

		if (query == null || query.isBlank()) {
			return List.of();
		}

		try {
			float[] queryVector = embeddingModel.embed(query).content().vector();

			validateVector(queryVector);

			return embeddingRepository.searchSimilar(userId, queryVector, DEFAULT_RESULT_LIMIT, MINIMUM_SIMILARITY);

		} catch (Exception e) {
			System.err.println("Semantic memory search failed: " + e.getMessage());

			try {
				// Text search has no vector similarity score.
				// A score of 0.0 indicates that no semantic score is available.
				return memoryRepo.searchByText(userId, query).stream()
						.map(memory -> new MemorySearchResult(memory.getId(), memory.getCategory(), memory.getContent(),
								memory.getSource(), memory.getImportance(), 0.0))
						.toList();

			} catch (Exception fallbackException) {
				System.err.println("Full-text memory search failed: " + fallbackException.getMessage());

				return List.of();
			}
		}
	}

	/**
	 * Backward-compatible retrieval method. Existing ChatService callers can
	 * continue using this method.
	 */
	@Transactional(readOnly = true)
	public List<String> searchRelevantMemories(UUID userId, String query) {

		return searchRelevantMemoryResults(userId, query).stream()
				.map(memory -> "[" + memory.category() + "] " + memory.content()).toList();
	}

	@Transactional
	public void deleteMemory(UUID userId, UUID memoryId) {

		Memory memory = memoryRepo.findByIdAndUserId(memoryId, userId)
				.orElseThrow(() -> new IllegalArgumentException("Memory not found"));

		memoryRepo.delete(memory);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateMemory(UUID userId, UUID memoryId, String newContent) {

		if (newContent == null || newContent.isBlank()) {
			throw new IllegalArgumentException("Memory content cannot be empty");
		}

		if (newContent.length() > 4000) {
			throw new IllegalArgumentException("Memory content cannot exceed 4000 characters");
		}

		Memory memory = memoryRepo.findByIdAndUserId(memoryId, userId)
				.orElseThrow(() -> new IllegalArgumentException("Memory not found"));

		memory.setContent(newContent.trim());

		Memory updatedMemory = memoryRepo.saveAndFlush(memory);

		float[] vector = embeddingModel.embed(updatedMemory.getContent()).content().vector();

		validateVector(vector);

		embeddingRepository.save(updatedMemory.getId(), vector);
	}

	private void validateVector(float[] vector) {

		if (vector == null || vector.length != 768) {
			throw new IllegalStateException(
					"Expected 768 embedding dimensions, but received " + (vector == null ? "null" : vector.length));
		}
	}

	private void validateCategory(String category) {

		if (category == null || !ALLOWED_CATEGORIES.contains(category.toUpperCase())) {

			throw new IllegalArgumentException("Invalid category. Allowed: " + String.join(", ", ALLOWED_CATEGORIES));
		}
	}

	private MemoryResponse toResponse(Memory memory) {

		return new MemoryResponse(memory.getId(), memory.getCategory(), memory.getContent(), memory.getSource(),
				memory.getImportance(), memory.getCreatedAt());
	}

	public record MemoryResponse(UUID id, String category, String content, String source, short importance,
			Instant createdAt) {
	}
}
