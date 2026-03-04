package entities;

public class Hotel {

    private int id;
    private String name;
    private String address;
    private int managerId;
    private int capacity;

    public Hotel() {}

    public Hotel(String name, String address, int managerId, int capacity) {
        this.name = name;
        this.address = address;
        this.managerId = managerId;
        this.capacity = capacity;
    }

    public Hotel(int id, String name, String address, int managerId, int capacity) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.managerId = managerId;
        this.capacity = capacity;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getManagerId() { return managerId; }
    public void setManagerId(int managerId) { this.managerId = managerId; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
}
