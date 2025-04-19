import javafx.animation.FadeTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.*;

public class MentorPage {
    private final int userId;
    private final String userName;

    public MentorPage(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public void show(Stage stage) {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color:#f0f2f5;");

        Tab profileTab = new Tab("Profile", getProfileForm(stage));
        Tab mentorshipTab = new Tab("Mentorship Requests", getMentorshipRequestsTab(stage));

        tabPane.getTabs().addAll(profileTab, mentorshipTab);
        tabPane.getSelectionModel().select(profileTab);

        Scene scene = new Scene(tabPane, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Mentor Dashboard - " + userName);
        stage.show();

        FadeTransition ft = new FadeTransition(Duration.millis(600), tabPane);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private VBox getProfileForm(Stage stage) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_LEFT);

        Label header = new Label("Mentor Profile");
        header.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        header.setTextFill(Color.web("#34495e"));

        TextField expertise = new TextField();
        TextField availability = new TextField();
        TextField email = new TextField();
        email.setDisable(true); // Email should not be editable

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM mentors WHERE user_id = ?");
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                expertise.setText(rs.getString("expertise"));
                availability.setText(rs.getString("availability"));
                email.setText(rs.getString("email"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Button updateButton = new Button("Update Profile");
        updateButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10 20;");
        updateButton.setOnAction(e -> updateProfile(expertise.getText(), availability.getText()));

        // Sign out Button
        Button signOutButton = new Button("Sign Out");
        signOutButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 10 20;");
        signOutButton.setOnAction(e -> signOut(stage));

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);

        form.add(new Label("Expertise:"), 0, 0);
        form.add(expertise, 1, 0);
        form.add(new Label("Availability:"), 0, 1);
        form.add(availability, 1, 1);
        form.add(new Label("Email:"), 0, 2);
        form.add(email, 1, 2);

        layout.getChildren().addAll(header, form, updateButton, signOutButton);
        return layout;
    }

    private void updateProfile(String expertise, String availability) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE mentors SET expertise = ?, availability = ? WHERE user_id = ?");
            stmt.setString(1, expertise);
            stmt.setString(2, availability);
            stmt.setInt(3, userId);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Profile updated successfully.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void signOut(Stage stage) {
        // Navigate to login page
        LoginPage loginPage = new LoginPage();
        loginPage.show(stage);  // Assuming LoginPage is your login page class
    }


    private VBox getMentorshipRequestsTab(Stage stage) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label header = new Label("Mentorship Requests");
        header.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        header.setTextFill(Color.web("#34495e"));

        TableView<Object[]> table = createModernTable();

        TableColumn<Object[], String> nameCol = new TableColumn<>("Mentee Name");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[0]));

        TableColumn<Object[], String> requestDescCol = new TableColumn<>("Request Description");
        requestDescCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[1]));

        TableColumn<Object[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[2]));

        TableColumn<Object[], Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button acceptButton = new Button("Accept");
            private final Button rejectButton = new Button("Reject");

            {
                acceptButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                rejectButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");

                acceptButton.setOnAction(event -> {
                    Object[] row = getTableRow().getItem();
                    int requestId = (Integer) row[3];
                    acceptRequest(requestId);
                    getTableView().getItems().remove(row);
                });

                rejectButton.setOnAction(event -> {
                    Object[] row = getTableRow().getItem();
                    int requestId = (Integer) row[3];
                    rejectRequest(requestId);
                    getTableView().getItems().remove(row);
                });
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox actionBox = new HBox(10, acceptButton, rejectButton);
                    actionBox.setAlignment(Pos.CENTER);
                    setGraphic(actionBox);
                }
            }
        });

        table.getColumns().addAll(nameCol, requestDescCol, statusCol, actionCol);

        // ✅ Fetch mentorship requests from the database
        try (Connection conn = DBConnection.getConnection()) {
            String sql =
                    "SELECT " +
                            "  r.support_name AS mentee_name, " +
                            "  r.idea_desc, " +
                            "  r.status, " +
                            "  r.id AS request_id " +
                            "FROM applications r " +
                            "JOIN mentor_requests m ON r.support_id = m.mentor_id " +
                            "WHERE r.support_id = ? " +
                            "  AND m.status = 'Mentor Pending'";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);  // your logged‑in mentor’s ID

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String menteeName = rs.getString("mentee_name");
                String requestDesc = rs.getString("idea_desc");
                String status = rs.getString("status");
                int requestId = rs.getInt("request_id");

                table.getItems().add(new Object[]{
                        menteeName,
                        requestDesc,
                        status,
                        requestId
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        layout.getChildren().addAll(header, table);
        return layout;
    }

    private void acceptRequest(int requestId) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE mentor_requests SET status = 'Accepted' WHERE id = ?");
            updateStmt.setInt(1, requestId);
            updateStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void rejectRequest(int requestId) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE mentor_requests SET status = 'Rejected' WHERE id = ?");
            updateStmt.setInt(1, requestId);
            updateStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private TableView<Object[]> createModernTable() {
        TableView<Object[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color:white; -fx-border-color:#ddd; -fx-border-radius:8; -fx-background-radius:8;");

        // Zebra striping
        table.setRowFactory(tv -> new TableRow<Object[]>() {
            @Override
            protected void updateItem(Object[] item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (getIndex() % 2 == 0) {
                    setStyle("-fx-background-color:#f9f9f9;");
                } else {
                    setStyle("");
                }
            }
        });
        return table;
    }
}