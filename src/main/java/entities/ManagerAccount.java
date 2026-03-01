package entities;

public class ManagerAccount {

    private final String managerId;
    private final String displayName;

    public ManagerAccount(String managerId, String displayName) {
        this.managerId = managerId;
        this.displayName = displayName;
    }

    public String getManagerId() {
        return managerId;
    }

    public String getDisplayName() {
        return displayName;
    }
}
