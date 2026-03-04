package com.esprit.furhope.api.web;

import com.esprit.furhope.api.dto.CommentDto;
import com.esprit.furhope.api.dto.CreateCommentRequest;
import com.esprit.furhope.api.dto.UpdateCommentRequest;
import com.esprit.furhope.api.service.ApiCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class CommentController {

    private final ApiCommentService commentService;

    public CommentController(ApiCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/{postId}/comments")
    public List<CommentDto> getComments(@PathVariable long postId) {
        return commentService.getPostComments(postId);
    }

    @PostMapping("/{postId}/comments")
    public CommentDto createComment(@PathVariable long postId, @RequestBody CreateCommentRequest request) {
        return commentService.createComment(postId, request);
    }

    @PutMapping("/comments/{commentId}")
    public Map<String, Boolean> updateComment(@PathVariable long commentId, @RequestBody UpdateCommentRequest request) {
        return Map.of("success", commentService.updateComment(commentId, request.getContent()));
    }

    @DeleteMapping("/comments/{commentId}")
    public Map<String, Boolean> deleteComment(@PathVariable long commentId) {
        return Map.of("success", commentService.deleteComment(commentId));
    }

    @DeleteMapping("/{postId}/comments")
    public Map<String, Boolean> deleteByPostId(@PathVariable long postId) {
        return Map.of("success", commentService.deleteByPostId(postId));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public Map<String, String> handleBadRequest(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}
