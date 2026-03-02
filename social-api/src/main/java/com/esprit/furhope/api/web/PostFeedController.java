package com.esprit.furhope.api.web;

import com.esprit.furhope.api.dto.PostDto;
import com.esprit.furhope.api.service.PostFeedService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostFeedController {

    private final PostFeedService postFeedService;

    public PostFeedController(PostFeedService postFeedService) {
        this.postFeedService = postFeedService;
    }

    @GetMapping("/feed")
    public List<PostDto> getFeed(@RequestParam("userId") long viewerId) {
        return postFeedService.getFeed(viewerId);
    }
}
