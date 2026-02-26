package com.esprit.furhope.utils;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    public static String formatAgo(Timestamp ts) {
        if (ts == null) return "";
        Instant then = ts.toInstant();
        Duration d = Duration.between(then, Instant.now());
        long s = d.getSeconds();

        // Handle timezone/clock skew safely: avoid showing "Just now" forever for future timestamps.
        if (s < -120) {
            LocalDateTime ldt = LocalDateTime.ofInstant(then, ZoneId.systemDefault());
            return ldt.format(DATE_TIME_FORMAT);
        }
        if (s < 0) s = 0;

        if (s < 60) return "Just now";
        if (s < 3600) return (s / 60) + "m ago";
        if (s < 86400) return (s / 3600) + "h ago";
        if (s < 2592000) return (s / 86400) + "d ago";
        LocalDateTime ldt = LocalDateTime.ofInstant(then, ZoneId.systemDefault());
        return ldt.format(DATE_TIME_FORMAT);
    }
}
