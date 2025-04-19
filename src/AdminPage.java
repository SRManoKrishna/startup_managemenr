import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;
public class AdminPage {

    public void start(Stage stage) {
        TabPane tabPane = new TabPane();

        tabPane.getTabs().addAll(
                createRoleTab("Mentors", "mentor"),
                createRoleTab("Investors", "investor"),
                createRoleTab("Founders", "founder"),
                createEventTab(),          // Events Tab
                createChartTab()           // Chart Tab
        );

        VBox root = new VBox(tabPane);
        root.setPadding(new Insets(10));

        stage.setTitle("Admin Dashboard");
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    // Create the Events Tab
    private Tab createEventTab() {
        Tab tab = new Tab("Events");

        TableView<EventData> table = new TableView<>();
        ObservableList<EventData> data = FXCollections.observableArrayList();

        TableColumn<EventData, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<EventData, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setCellFactory(TextFieldTableCell.forTableColumn());
        titleCol.setOnEditCommit(e -> {
            EventData event = e.getRowValue();
            event.setTitle(e.getNewValue());
            updateEvent(event);
        });

        TableColumn<EventData, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionCol.setCellFactory(TextFieldTableCell.forTableColumn());
        descriptionCol.setOnEditCommit(e -> {
            EventData event = e.getRowValue();
            event.setDescription(e.getNewValue());
            updateEvent(event);
        });

        TableColumn<EventData, String> eventDateCol = new TableColumn<>("Event Date");
        eventDateCol.setCellValueFactory(new PropertyValueFactory<>("eventDate"));
        eventDateCol.setCellFactory(TextFieldTableCell.forTableColumn());
        eventDateCol.setOnEditCommit(e -> {
            EventData event = e.getRowValue();
            event.setEventDate(e.getNewValue());
            updateEvent(event);
        });

        TableColumn<EventData, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        locationCol.setCellFactory(TextFieldTableCell.forTableColumn());
        locationCol.setOnEditCommit(e -> {
            EventData event = e.getRowValue();
            event.setLocation(e.getNewValue());
            updateEvent(event);
        });

        table.getColumns().addAll(idCol, titleCol, descriptionCol, eventDateCol, locationCol);
        table.setEditable(true);
        table.setItems(data);

        Button addBtn = new Button("Add Event");
        addBtn.setOnAction(e -> showAddEventDialog(data));

        Button delBtn = new Button("Delete Selected Event");
        delBtn.setOnAction(e -> {
            EventData selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteEvent(selected);
                data.remove(selected);
            }
        });

        loadEvents(data);

        VBox layout = new VBox(10, table, new HBox(10, addBtn, delBtn));
        layout.setPadding(new Insets(10));
        tab.setContent(layout);
        return tab;
    }

    // Create the Chart Tab
    private Tab createChartTab() {
        Tab tab = new Tab("Progress Chart");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Status");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount");

        BarChart<String, Number> fundingChart = new BarChart<>(xAxis, yAxis);

        XYChart.Series<String, Number> fundingSeries = new XYChart.Series<>();
        fundingSeries.setName("Funding Amounts");

        // Load funding data (example for visualization)
        loadFundingData(fundingSeries);

        fundingChart.getData().add(fundingSeries);

        VBox layout = new VBox(10, fundingChart);
        layout.setPadding(new Insets(10));
        tab.setContent(layout);
        return tab;
    }

    // Load Events from Database
    private void loadEvents(ObservableList<EventData> data) {
        data.clear();
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT id, title, description, event_date, location FROM events";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new EventData(rs.getInt("id"), rs.getString("title"),
                        rs.getString("description"), rs.getString("event_date"),
                        rs.getString("location")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Add Event Dialog
    private void showAddEventDialog(ObservableList<EventData> data) {
        Stage dialog = new Stage();
        dialog.setTitle("Add New Event");

        TextField titleField = new TextField();
        titleField.setPromptText("Event Title");

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Event Description");

        TextField eventDateField = new TextField();
        eventDateField.setPromptText("Event Date");

        TextField locationField = new TextField();
        locationField.setPromptText("Event Location");

        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> {
            String title = titleField.getText();
            String description = descriptionField.getText();
            String eventDate = eventDateField.getText();
            String location = locationField.getText();

            if (!title.isEmpty() && !description.isEmpty() && !eventDate.isEmpty() && !location.isEmpty()) {
                try (Connection conn = DBConnection.getConnection()) {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO events (title, description, event_date, location) VALUES (?, ?, ?, ?)",
                            new String[] {"id"});
                    ps.setString(1, title);
                    ps.setString(2, description);
                    ps.setString(3, eventDate);
                    ps.setString(4, location);
                    ps.executeUpdate();

                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        int eventId = rs.getInt(1);
                        data.add(new EventData(eventId, title, description, eventDate, location));
                        dialog.close();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        VBox layout = new VBox(10, titleField, descriptionField, eventDateField, locationField, saveBtn);
        layout.setPadding(new Insets(20));
        dialog.setScene(new Scene(layout, 300, 250));
        dialog.show();
    }

    // Update Event
    private void updateEvent(EventData event) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE events SET title=?, description=?, event_date=?, location=? WHERE id=?");
            ps.setString(1, event.getTitle());
            ps.setString(2, event.getDescription());
            ps.setString(3, event.getEventDate());
            ps.setString(4, event.getLocation());
            ps.setInt(5, event.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Delete Event
    private void deleteEvent(EventData event) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM events WHERE id=?");
            ps.setInt(1, event.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load Funding Data for Chart
    private void loadFundingData(XYChart.Series<String, Number> fundingSeries) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT status, SUM(amount) as total_amount FROM funding GROUP BY status";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                fundingSeries.getData().add(new XYChart.Data<>(rs.getString("status"), rs.getDouble("total_amount")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class EventData {
        private final Integer id;
        private String title, description, eventDate, location;

        public EventData(Integer id, String title, String description, String eventDate, String location) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.eventDate = eventDate;
            this.location = location;
        }

        public Integer getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getEventDate() { return eventDate; }
        public String getLocation() { return location; }
        public void setTitle(String title) { this.title = title; }
        public void setDescription(String description) { this.description = description; }
        public void setEventDate(String eventDate) { this.eventDate = eventDate; }
        public void setLocation(String location) { this.location = location; }
    }
    private Tab createRoleTab(String title, String role) {
        Tab tab = new Tab(title);
        TableView<UserData> table = new TableView<>();
        ObservableList<UserData> data = FXCollections.observableArrayList();

        TableColumn<UserData, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<UserData, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> {
            UserData u = e.getRowValue();
            u.setName(e.getNewValue());
            updateUser(u, role);
        });

        TableColumn<UserData, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setCellFactory(TextFieldTableCell.forTableColumn());
        emailCol.setOnEditCommit(e -> {
            UserData u = e.getRowValue();
            u.setEmail(e.getNewValue());
            updateUser(u, role);
        });

        table.getColumns().addAll(idCol, nameCol, emailCol);
        table.setEditable(true);
        // Enable column-level editing
        nameCol.setEditable(true);
        emailCol.setEditable(true);
        table.setItems(data);

        Button addBtn = new Button("Add " + title);
        addBtn.setOnAction(e -> showAddDialog(role, data));

        Button delBtn = new Button("Delete Selected");
        delBtn.setOnAction(e -> {
            UserData selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteUser(selected, role);
                data.remove(selected);
            }
        });

        loadUsersByRole(data, role);

        VBox layout = new VBox(10, table, new HBox(10, addBtn, delBtn));
        layout.setPadding(new Insets(10));
        tab.setContent(layout);
        return tab;
    }

    private void loadUsersByRole(ObservableList<UserData> data, String role) {
        data.clear();
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT id, name, email FROM users WHERE role = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            // Use exact-case role to satisfy CHECK constraint
            String dbRole = switch (role.toLowerCase()) {
                case "admin" -> "Admin";
                case "investor" -> "Investor";
                case "mentor" -> "Mentor";
                case "founder" -> "Founder";
                default -> role;
            };
            ps.setString(1, dbRole);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new UserData(rs.getInt("id"), rs.getString("name"), rs.getString("email")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAddDialog(String role, ObservableList<UserData> data) {
        Stage dialog = new Stage();
        dialog.setTitle("Add New " + role);

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();

            if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
                try (Connection conn = DBConnection.getConnection()) {
                    // Normalize role to match CHECK constraint
                    String dbRole = switch (role.toLowerCase()) {
                        case "admin" -> "Admin";
                        case "investor" -> "Investor";
                        case "mentor" -> "Mentor";
                        case "founder" -> "Founder";
                        default -> role;
                    };

                    // Insert into users table
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)",
                            new String[]{"id"}
                    );
                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setString(3, password);
                    ps.setString(4, dbRole);
                    ps.executeUpdate();

                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        int userId = rs.getInt(1);

                        // Insert into role-specific table
                        PreparedStatement roleStmt = conn.prepareStatement(
                                "INSERT INTO " + role + "s (user_id, name, email) VALUES (?, ?, ?)"
                        );
                        roleStmt.setInt(1, userId);
                        roleStmt.setString(2, name);
                        roleStmt.setString(3, email);
                        roleStmt.executeUpdate();

                        data.add(new UserData(userId, name, email));
                        dialog.close();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        VBox layout = new VBox(10, nameField, emailField, passwordField, saveBtn);
        layout.setPadding(new Insets(20));
        dialog.setScene(new Scene(layout, 300, 200));
        dialog.show();
    }

    private void updateUser(UserData user, String role) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE users SET name=?, email=? WHERE id=?");
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setInt(3, user.getId());
            ps.executeUpdate();

            PreparedStatement roleStmt = conn.prepareStatement("UPDATE " + role + "s SET name=?, email=? WHERE user_id=?");
            roleStmt.setString(1, user.getName());
            roleStmt.setString(2, user.getEmail());
            roleStmt.setInt(3, user.getId());
            roleStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteUser(UserData user, String role) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM " + role + "s WHERE user_id=?");
            ps1.setInt(1, user.getId());
            ps1.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM users WHERE id=?");
            ps2.setInt(1, user.getId());
            ps2.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class UserData {
        private final Integer id;
        private String name, email;

        public UserData(Integer id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public Integer getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public void setName(String name) { this.name = name; }
        public void setEmail(String email) { this.email = email; }
    }
}
