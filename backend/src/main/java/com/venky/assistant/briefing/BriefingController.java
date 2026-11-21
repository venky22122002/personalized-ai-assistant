package com.venky.assistant.briefing;

import com.venky.assistant.task.TaskService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/briefing")
public class BriefingController {

    private final TaskService taskService;

    public BriefingController(TaskService taskService) {
        this.taskService = taskService;
    }

    public record BriefingResponse(String date, String greeting,
                                    List<TaskService.TaskResponse> openTasks,
                                    int totalOpen, int highPriority) {}

    @GetMapping("/today")
    public BriefingResponse today(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();

        List<TaskService.TaskResponse> open = taskService.getTasks(userId, true);
        long high = open.stream().filter(t -> "HIGH".equals(t.priority())).count();

        String dateStr = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"));

        int hour = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).getHour();
        String greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";

        return new BriefingResponse(dateStr, greeting, open, open.size(), (int) high);
    }
}
