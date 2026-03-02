package com.esprit.furhope.api.web;

import com.esprit.furhope.api.dto.FriendDto;
import com.esprit.furhope.api.dto.FriendRequestDto;
import com.esprit.furhope.api.service.FriendApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendApiService friendApiService;

    public FriendController(FriendApiService friendApiService) {
        this.friendApiService = friendApiService;
    }

    @PostMapping("/request")
    public Map<String, Boolean> sendRequest(@RequestBody FriendRequestDto request) {
        boolean ok = friendApiService.sendRequest(request.getSenderId(), request.getReceiverId());
        return Map.of("success", ok);
    }

    @PostMapping("/accept")
    public Map<String, Boolean> acceptRequest(@RequestBody FriendRequestDto request) {
        boolean ok = friendApiService.acceptRequest(request.getReceiverId(), request.getSenderId());
        return Map.of("success", ok);
    }

    @PostMapping("/decline")
    public Map<String, Boolean> declineRequest(@RequestBody FriendRequestDto request) {
        boolean ok = friendApiService.declineRequest(request.getReceiverId(), request.getSenderId());
        return Map.of("success", ok);
    }

    @GetMapping("/list")
    public List<FriendDto> getFriends(@RequestParam("userId") long userId) {
        return friendApiService.getFriends(userId);
    }

    @GetMapping("/incoming")
    public List<FriendDto> incoming(@RequestParam("userId") long userId) {
        return friendApiService.getIncomingRequests(userId);
    }

    @GetMapping("/search")
    public List<FriendDto> search(@RequestParam("userId") long userId, @RequestParam("query") String query) {
        return friendApiService.searchUsersByName(userId, query);
    }
}
