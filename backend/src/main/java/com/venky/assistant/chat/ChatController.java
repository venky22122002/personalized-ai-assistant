
package com.venky.assistant.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.venky.assistant.idempotency.ChatRequestIdempotency;
import com.venky.assistant.idempotency.ChatRequestIdempotencyService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

	private final ChatService chatService;
	private final ChatRequestIdempotencyService idempotencyService;
	private final ObjectMapper objectMapper;

	public ChatController(ChatService chatService, ChatRequestIdempotencyService idempotencyService,
			ObjectMapper objectMapper) {
		this.chatService = chatService;
		this.idempotencyService = idempotencyService;
		this.objectMapper = objectMapper;
	}

	public record ChatRequest(UUID conversationId, @NotBlank @Size(max = 8000) String message) {
	}

	@PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ChatService.ChatResponse> chat(@Valid @RequestBody ChatRequest request,
			@RequestHeader("Idempotency-Key") UUID requestId, Authentication auth) {

		UUID userId = (UUID) auth.getPrincipal();

		String requestHash = idempotencyService.hashRequest(request.conversationId(), request.message());

		boolean reserved;

		try {
			reserved = idempotencyService.reserve(userId, requestId, requestHash);
		} catch (DataIntegrityViolationException ex) {
			// Another concurrent request may have reserved this key first.
			return existingResponse(userId, requestId, requestHash);
		}

		if (!reserved) {
			return existingResponse(userId, requestId, requestHash);
		}

		try {
			ChatService.ChatResponse response = chatService.chat(userId, request.conversationId(), request.message(),
					requestId);

			return ResponseEntity.ok(response);

		} catch (RuntimeException ex) {
			idempotencyService.markFailed(userId, requestId);
			throw ex;
		}
	}

	private ResponseEntity<ChatService.ChatResponse> existingResponse(UUID userId, UUID requestId, String requestHash) {

		ChatRequestIdempotency existing;

		try {
			existing = idempotencyService.find(userId, requestId);
		} catch (IllegalStateException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Request is being reserved; retry with the same Idempotency-Key");
		}

		if (!existing.getRequestHash().equals(requestHash)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Idempotency-Key was already used with a different request");
		}

		if ("COMPLETED".equals(existing.getStatus())) {
			JsonNode savedResponse = existing.getResponse();

			if (savedResponse == null || savedResponse.isNull()) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Completed request has no saved response");
			}

			ChatService.ChatResponse response = objectMapper.convertValue(savedResponse,
					ChatService.ChatResponse.class);

			return ResponseEntity.ok(response);
		}

		if ("FAILED".equals(existing.getStatus())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"The previous request failed. Submit a new request with a new Idempotency-Key");
		}

		throw new ResponseStatusException(HttpStatus.CONFLICT,
				"Request is still processing. Retry with the same Idempotency-Key");
	}

	@GetMapping("/conversations")
	public List<ChatService.ConversationSummary> listConversations(Authentication auth) {
		UUID userId = (UUID) auth.getPrincipal();
		return chatService.listConversations(userId);
	}

	@GetMapping("/conversations/{id}/messages")
	public List<ChatService.MessageResponse> getMessages(@PathVariable UUID id, Authentication auth) {
		UUID userId = (UUID) auth.getPrincipal();
		return chatService.getMessages(userId, id);
	}
}
