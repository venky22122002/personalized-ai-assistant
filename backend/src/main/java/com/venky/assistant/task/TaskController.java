package com.venky.assistant.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    public record CreateTaskRequest(
            @NotBlank @Size(max = 250) String title,
            String description,
            String priority,
            Instant dueAt
    ) {}

    public record PatchTaskRequest(
            String title,
            String description,
            String status,
            String priority,
            Instant dueAt
    ) {}

    @PostMapping
    public TaskService.TaskResponse create(
            @Valid @RequestBody CreateTaskRequest req,
            Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return taskService.createTask(userId, req.title(), req.description(),
                req.priority(), req.dueAt());
    }

    @GetMapping
    public List<TaskService.TaskResponse> list(
            @RequestParam(defaultValue = "true") boolean openOnly,
            Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return taskService.getTasks(userId, openOnly);
    }

    @PatchMapping("/{id}")
    public TaskService.TaskResponse update(
            @PathVariable UUID id,
            @RequestBody PatchTaskRequest req,
            Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return taskService.updateTask(userId, id,
                req.title(), req.description(), req.status(),
                req.priority(), req.dueAt());
    }
}
