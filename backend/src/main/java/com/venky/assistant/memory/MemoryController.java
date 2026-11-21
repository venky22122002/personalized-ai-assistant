package com.venky.assistant.memory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memories")
public class MemoryController {

	private final MemoryService memoryService;

	public MemoryController(MemoryService memoryService) {
		this.memoryService = memoryService;
	}

	public record CreateMemoryRequest(@NotBlank String category, @NotBlank @Size(max = 4000) String content,
			String source, @Min(1) @Max(5) short importance) {
	}

	@PostMapping
	public MemoryService.MemoryResponse create(@Valid @RequestBody CreateMemoryRequest req, Authentication auth) {
		UUID userId = (UUID) auth.getPrincipal();
		return memoryService.saveMemory(userId, req.category(), req.content(),
				req.source() != null ? req.source() : "USER", req.importance() == 0 ? 3 : req.importance());
	}

	@GetMapping
	public List<MemoryService.MemoryResponse> list(@RequestParam(required = false) String category,
			Authentication auth) {
		UUID userId = (UUID) auth.getPrincipal();
		return memoryService.getAllMemories(userId, category);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
		UUID userId = (UUID) auth.getPrincipal();
		memoryService.deleteMemory(userId, id);
		return ResponseEntity.noContent().build();
	}
}
