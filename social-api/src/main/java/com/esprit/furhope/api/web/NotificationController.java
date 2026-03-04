package com.esprit.furhope.api.web;

import com.esprit.furhope.api.dto.MarkAllReadRequest;
import com.esprit.furhope.api.dto.NotificationDto;
import com.esprit.furhope.api.service.ApiNotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final ApiNotificationService notificationService;

    public NotificationController(ApiNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationDto> getLatest(
            @RequestParam("userId") int userId,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        int effectiveLimit = (limit == null ? 30 : limit);
        return notificationService.getLatest(userId, effectiveLimit);
    }

    @GetMapping("/unreadCount")
    public Map<String, Integer> countUnread(@RequestParam("userId") int userId) {
        return Map.of("count", notificationService.countUnread(userId));
    }

    @PostMapping("/markAllRead")
    public Map<String, String> markAllRead(@RequestBody MarkAllReadRequest request) {
        notificationService.markAllRead(request.getUserId());
        return Map.of("status", "ok");
    }
}
