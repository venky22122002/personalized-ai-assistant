package com.venky.assistant.action;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/actions")
public class ActionController {

	private final ActionService actionService;

	public ActionController(ActionService actionService) {
		this.actionService = actionService;
	}

	@GetMapping
	public List<ActionService.ActionResponse> list(@RequestParam(defaultValue = "true") boolean pendingOnly,
			Authentication auth) {
		UUID userId = (UUID) auth.getPrincipal();
		return pendingOnly ? actionService.getPendingActions(userId) : actionService.getAllActions(userId);
	}

	@PostMapping("/{id}/approve")
	public ResponseEntity<ActionService.ActionResponse> approve(@PathVariable UUID id, Authentication auth) {
		UUID userId = (UUID) auth.getPrincipal();
		return ResponseEntity.ok(actionService.approve(userId, id));
	}

	@PostMapping("/{id}/reject")
	public ResponseEntity<ActionService.ActionResponse> reject(@PathVariable UUID id, Authentication auth) {
		UUID userId = (UUID) auth.getPrincipal();
		return ResponseEntity.ok(actionService.reject(userId, id));
	}

	@PostMapping("/propose")
	public ResponseEntity<ActionService.ActionResponse> propose(@RequestBody JsonNode request, Authentication auth) {

		UUID userId = (UUID) auth.getPrincipal();

		JsonNode actionTypeNode = request.get("actionType");
		JsonNode payload = request.get("payload");

		if (actionTypeNode == null || !actionTypeNode.isTextual() || actionTypeNode.asText().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "actionType is required");
		}

		if (payload == null || !payload.isObject()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload must be a JSON object");
		}

		ActionService.ActionResponse response = actionService.propose(userId,
				actionTypeNode.asText().trim().toUpperCase(), payload);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

}
