package com.esprit.furhope.api.web;

import com.esprit.furhope.api.dto.CreatePostRequest;
import com.esprit.furhope.api.dto.PostDto;
import com.esprit.furhope.api.service.PostApiService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostCrudController {

    private final PostApiService postApiService;

    public PostCrudController(PostApiService postApiService) {
        this.postApiService = postApiService;
    }

    @PostMapping
    public PostDto create(@RequestBody CreatePostRequest request) {
        return postApiService.create(request);
    }

    @PutMapping("/{id}")
    public Map<String, Boolean> update(@PathVariable long id, @RequestBody CreatePostRequest request) {
        return Map.of("success", postApiService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@PathVariable long id) {
        return Map.of("success", postApiService.delete(id));
    }

    @GetMapping("/firstId")
    public Map<String, Long> firstId() {
        Long id = postApiService.getFirstPostId();
        return Map.of("id", id == null ? -1L : id);
    }
}
