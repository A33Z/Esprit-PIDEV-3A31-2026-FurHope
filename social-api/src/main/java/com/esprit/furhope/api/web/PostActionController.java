package com.esprit.furhope.api.web;

import com.esprit.furhope.api.dto.ReportRequest;
import com.esprit.furhope.api.dto.ShareRequest;
import com.esprit.furhope.api.service.PostActionApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostActionController {

    private final PostActionApiService service;

    public PostActionController(PostActionApiService service) {
        this.service = service;
    }

    @GetMapping("/{postId}/share")
    public Map<String, Boolean> hasShared(@PathVariable long postId, @RequestParam("userId") long userId) {
        return Map.of("shared", service.hasShared(postId, userId));
    }

    @PostMapping("/{postId}/share")
    public Map<String, Boolean> toggleShare(@PathVariable long postId, @RequestBody ShareRequest request) {
        return Map.of("shared", service.toggleShare(postId, request.getUserId()));
    }

    @GetMapping("/{postId}/report/status")
    public Map<String, Boolean> hasReported(@PathVariable long postId, @RequestParam("userId") long userId) {
        return Map.of("reported", service.hasReported(postId, userId));
    }

    @PostMapping("/{postId}/report")
    public Map<String, Boolean> report(@PathVariable long postId, @RequestBody ReportRequest request) {
        return Map.of("success", service.reportPost(postId, request.getUserId(), request.getReason()));
    }
}
