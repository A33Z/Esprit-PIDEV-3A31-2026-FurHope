package entities;

public class User {

    private final int id;
    private final String displayName;
    private final Role role;

    public User(int id, String displayName, Role role) {
        this.id = id;
        this.displayName = displayName;
        this.role = role;
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
}
