package com.esprit.furhope.services;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SocialSchemaService {

    private static boolean initialized;

    private SocialSchemaService() {
    }

    public static synchronized void ensureSocialSchema(Connection connection) throws SQLException {
        if (initialized || connection == null) {
            return;
        }

        ensureFriendRequestTable(connection);
        ensureFriendshipTable(connection);
        ensurePostTable(connection);
        ensureCommentTable(connection);
        ensureCommentReactionTable(connection);
        ensureNotificationTable(connection);
        ensurePostReactionTable(connection);
        ensurePostShareTable(connection);
        ensurePostReportTable(connection);
        initialized = true;
    }

    private static void ensureFriendRequestTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS friend_request (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    sender_id INT NOT NULL,
                    receiver_id INT NOT NULL,
                    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void ensureFriendshipTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS friendship (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user1_id INT NOT NULL,
                    user2_id INT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uq_friendship_pair (user1_id, user2_id)
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void ensurePostTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS post (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    author_id INT NOT NULL,
                    caption TEXT NOT NULL,
                    media_type VARCHAR(16) NOT NULL DEFAULT 'NONE',
                    media_path VARCHAR(1024) NULL,
                    thumbnail_path VARCHAR(1024) NULL,
                    duration_seconds INT NULL,
                    likes_count INT NOT NULL DEFAULT 0,
                    dislikes_count INT NOT NULL DEFAULT 0,
                    shares_count INT NOT NULL DEFAULT 0,
                    comments_count INT NOT NULL DEFAULT 0,
                    visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
                    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void ensureCommentTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS comment (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    post_id BIGINT NOT NULL,
                    author_id INT NOT NULL,
                    parent_comment_id BIGINT NULL,
                    body TEXT NOT NULL,
                    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_comment_post
                        FOREIGN KEY (post_id) REFERENCES post(id)
                        ON DELETE CASCADE,
                    CONSTRAINT fk_comment_parent
                        FOREIGN KEY (parent_comment_id) REFERENCES comment(id)
                        ON DELETE CASCADE
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void ensureNotificationTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS notification (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    recipient_id INT NOT NULL,
                    actor_id INT NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    post_id BIGINT NULL,
                    comment_id BIGINT NULL,
                    message VARCHAR(255) NULL,
                    is_read TINYINT(1) NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void ensureCommentReactionTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS comment_reaction (
                    comment_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    reaction VARCHAR(16) NOT NULL,
                    PRIMARY KEY (comment_id, user_id),
                    CONSTRAINT fk_comment_reaction_comment
                        FOREIGN KEY (comment_id) REFERENCES comment(id)
                        ON DELETE CASCADE
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void ensurePostReactionTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS post_reaction (
                    post_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    reaction VARCHAR(16) NOT NULL,
                    PRIMARY KEY (post_id, user_id),
                    CONSTRAINT fk_reaction_post
                        FOREIGN KEY (post_id) REFERENCES post(id)
                        ON DELETE CASCADE
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void ensurePostShareTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS post_share (
                    post_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (post_id, user_id),
                    CONSTRAINT fk_share_post
                        FOREIGN KEY (post_id) REFERENCES post(id)
                        ON DELETE CASCADE
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void ensurePostReportTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS post_report (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    post_id BIGINT NOT NULL,
                    reporter_user_id BIGINT NOT NULL,
                    reason VARCHAR(255) NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uq_post_report (post_id, reporter_user_id),
                    CONSTRAINT fk_report_post
                        FOREIGN KEY (post_id) REFERENCES post(id)
                        ON DELETE CASCADE
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
