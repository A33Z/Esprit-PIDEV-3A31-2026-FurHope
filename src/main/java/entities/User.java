package entities;

public class User {

    private final int id;
    private final String displayName;
    private final Role role;
    private final String principalId;

    public User(int id, String displayName, Role role) {
        this(id, displayName, role, null);
    }

    public User(int id, String displayName, Role role, String principalId) {
        this.id = id;
        this.displayName = displayName;
        this.role = role;
        this.principalId = principalId;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public String getPrincipalId() {
        return principalId;
    }
}
