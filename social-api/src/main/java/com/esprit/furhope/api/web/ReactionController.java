package com.esprit.furhope.api.web;

import com.esprit.furhope.api.dto.ReactionDto;
import com.esprit.furhope.api.dto.ReactionToggleRequest;
import com.esprit.furhope.api.service.ReactionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @GetMapping("/{postId}/reaction")
    public ReactionDto getReaction(@PathVariable long postId, @RequestParam("userId") long userId) {
        String reaction = reactionService.getUserReaction(postId, userId);
        return new ReactionDto(postId, userId, reaction);
    }

    @PostMapping("/{postId}/reaction")
    public ReactionDto toggleReaction(@PathVariable long postId, @RequestBody ReactionToggleRequest request) {
        return reactionService.toggleReaction(postId, request.getUserId(), request.getReaction());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public java.util.Map<String, String> handleBadRequest(IllegalArgumentException ex) {
        return java.util.Map.of("error", ex.getMessage());
    }
}
