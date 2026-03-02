package com.esprit.furhope.services.api;

import com.esprit.furhope.integration.api.ApiClient;
import com.esprit.furhope.integration.api.dto.ApiResult;
import com.esprit.furhope.integration.api.dto.FriendDto;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiFriendService {

    public static class UserMini {
        private final int id;
        private final String name;

        public UserMini(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    private final ApiClient apiClient = new ApiClient();

    public boolean sendRequest(int senderId, int receiverId) throws SQLException {
        try {
            ApiResult response = apiClient.post(
                    "/api/friends/request",
                    null,
                    Map.of("senderId", senderId, "receiverId", receiverId),
                    ApiResult.class
            );
            return response != null && response.isSuccess();
        } catch (IOException e) {
            throw asSqlException("Failed to send friend request", e);
        }
    }

    public boolean acceptRequest(int receiverId, int senderId) throws SQLException {
        try {
            ApiResult response = apiClient.post(
                    "/api/friends/accept",
                    null,
                    Map.of("senderId", senderId, "receiverId", receiverId),
                    ApiResult.class
            );
            return response != null && response.isSuccess();
        } catch (IOException e) {
            throw asSqlException("Failed to accept friend request", e);
        }
    }

    public boolean declineRequest(int receiverId, int senderId) throws SQLException {
        try {
            ApiResult response = apiClient.post(
                    "/api/friends/decline",
                    null,
                    Map.of("senderId", senderId, "receiverId", receiverId),
                    ApiResult.class
            );
            return response != null && response.isSuccess();
        } catch (IOException e) {
            throw asSqlException("Failed to decline friend request", e);
        }
    }

    public List<UserMini> getFriends(int userId) throws SQLException {
        try {
            List<FriendDto> dtos = apiClient.get(
                    "/api/friends/list",
                    Map.of("userId", userId),
                    new TypeReference<List<FriendDto>>() {
                    }
            );
            return toMiniList(dtos);
        } catch (IOException e) {
            throw asSqlException("Failed to get friends", e);
        }
    }

    public List<UserMini> getIncomingRequests(int userId) throws SQLException {
        try {
            List<FriendDto> dtos = apiClient.get(
                    "/api/friends/incoming",
                    Map.of("userId", userId),
                    new TypeReference<List<FriendDto>>() {
                    }
            );
            return toMiniList(dtos);
        } catch (IOException e) {
            throw asSqlException("Failed to get incoming requests", e);
        }
    }

    public List<UserMini> searchUsersByName(String query, int excludeUserId) throws SQLException {
        try {
            List<FriendDto> dtos = apiClient.get(
                    "/api/friends/search",
                    Map.of("userId", excludeUserId, "query", query == null ? "" : query),
                    new TypeReference<List<FriendDto>>() {
                    }
            );
            return toMiniList(dtos);
        } catch (IOException e) {
            throw asSqlException("Failed to search users", e);
        }
    }

    public UserMini findUserByEmail(String email) throws SQLException {
        List<UserMini> matches = searchUsersByName(email, -1);
        return matches.isEmpty() ? null : matches.get(0);
    }

    public boolean hasPendingRequest(int userA, int userB) throws SQLException {
        List<UserMini> incoming = getIncomingRequests(userB);
        return incoming.stream().anyMatch(u -> u.getId() == userA);
    }

    private List<UserMini> toMiniList(List<FriendDto> dtos) {
        List<UserMini> out = new ArrayList<>();
        if (dtos == null) return out;
        for (FriendDto dto : dtos) {
            out.add(new UserMini((int) dto.getId(), dto.getName()));
        }
        return out;
    }

    private SQLException asSqlException(String message, Exception cause) {
        return new SQLException(message + ": " + cause.getMessage(), cause);
    }
}
