
package com.venky.assistant.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class ChatRequestIdempotencyService {

	private final ChatRequestIdempotencyRepository repository;

	public ChatRequestIdempotencyService(ChatRequestIdempotencyRepository repository) {
		this.repository = repository;
	}

	public String hashRequest(UUID conversationId, String message) {
		String normalizedConversationId = conversationId == null ? "NEW_CONVERSATION" : conversationId.toString();

		String input = normalizedConversationId + "\n" + message;

		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));

			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean reserve(UUID userId, UUID requestId, String requestHash) {
		var existing = repository.findByUserIdAndRequestId(userId, requestId);

		if (existing.isPresent()) {
			return false;
		}

		repository.saveAndFlush(new ChatRequestIdempotency(userId, requestId, requestHash));

		return true;
	}

	@Transactional(readOnly = true)
	public ChatRequestIdempotency find(UUID userId, UUID requestId) {
		return repository.findByUserIdAndRequestId(userId, requestId)
				.orElseThrow(() -> new IllegalStateException("Idempotency record was not found"));
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void complete(UUID userId, UUID requestId, JsonNode response) {
		ChatRequestIdempotency record = repository.findByUserIdAndRequestId(userId, requestId)
				.orElseThrow(() -> new IllegalStateException("Idempotency record not found"));

		record.markCompleted(response);
		repository.saveAndFlush(record);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(UUID userId, UUID requestId) {
		repository.findByUserIdAndRequestId(userId, requestId).ifPresent(record -> {
			record.markFailed();
			repository.saveAndFlush(record);
		});
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean markStaleRequestFailed(UUID userId, UUID requestId, Instant cutoff) {

		ChatRequestIdempotency record = repository.findByUserIdAndRequestIdForUpdate(userId, requestId).orElse(null);

		if (record == null) {
			return false;
		}

		if (!"PROCESSING".equals(record.getStatus())) {
			return false;
		}

		if (!record.getUpdatedAt().isBefore(cutoff)) {
			return false;
		}

		record.markFailed();
		repository.saveAndFlush(record);
		return true;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void lockProcessingRequest(UUID userId, UUID requestId) {
		ChatRequestIdempotency record = repository.findByUserIdAndRequestIdForUpdate(userId, requestId)
				.orElseThrow(() -> new IllegalStateException("Idempotency record not found"));

		if (!"PROCESSING".equals(record.getStatus())) {
			throw new IllegalStateException("Request is no longer in PROCESSING state");
		}
	}
}
