public class User {
    private int id;
    private String name;
    private String email;
    private String password; // Password field for user
    private String role;     // Role of the user (e.g., Mentor, Founder, Investor)
    private String extra;    // Additional data specific to the role (e.g., expertise, startup name, budget)

    // Full constructor matching the AdminPage logic
    public User(int id, String name, String email, String password, String role, String extra) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.extra = extra;
    }

    // Constructor without 'extra' for simpler use cases
    public User(int id, String name, String email, String password, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // Constructor for editing scenarios
    public User(int id, String name, String extra) {
        this.id = id;
        this.name = name;
        this.extra = extra;
    }

    // Getters and setters for all fields
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}