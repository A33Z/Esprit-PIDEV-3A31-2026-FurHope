package services;

import java.util.regex.Pattern;

public final class ManagerIdPolicy {

    private static final Pattern MANAGER_ID_PATTERN =
            Pattern.compile("(?i)^(ADMIN|HM-[A-Z0-9_-]{3,}|[A-Z0-9_-]*MANAGER[A-Z0-9_-]*)$");

    private ManagerIdPolicy() {
    }

    public static boolean isValid(String managerId) {
        if (managerId == null) {
            return false;
        }
        return MANAGER_ID_PATTERN.matcher(managerId.trim()).matches();
    }
}
