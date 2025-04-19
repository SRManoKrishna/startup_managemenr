import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.*;
import java.util.regex.Pattern;

public class LoginPage {

    public void show(Stage stage) {
        Label title = new Label("Login");
        title.setFont(Font.font("Arial", 24));
        title.setTextFill(Color.CYAN);

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Login");
        loginButton.setStyle("-fx-background-color: #0ff; -fx-text-fill: #000;");
        loginButton.setOnAction(e -> login(stage, emailField.getText(), passwordField.getText()));

        Button signUpButton = new Button("Sign Up");
        signUpButton.setStyle("-fx-background-color: #0ff; -fx-text-fill: #000;");
        signUpButton.setOnAction(e -> showSignUpPage(stage));

        VBox layout = new VBox(10, title, emailField, passwordField, loginButton, signUpButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: black;");

        FadeTransition fade = new FadeTransition(Duration.seconds(2), layout);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        Scene scene = new Scene(layout, 400, 300);
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.show();
    }

    private void login(Stage stage, String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Please fill in both email and password.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT id, name, role FROM users WHERE email = ? AND password = ?");
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String userName = rs.getString("name");
                String role = rs.getString("role");

                if ("Founder".equalsIgnoreCase(role)) {
                    FounderPage founderPage = new FounderPage(userId, userName);
                    founderPage.show(stage);
                } else if ("Investor".equalsIgnoreCase(role)) {
                    InvestorPage investorPage = new InvestorPage(userId,userName);
                    investorPage.show(stage);
                } else if ("Mentor".equalsIgnoreCase(role)) {
                    MentorPage mentorPage = new MentorPage(userId,userName);
                    mentorPage.show(stage);
                }
                else if ("Admin".equalsIgnoreCase(role)) {
                    AdminPage adminPage = new AdminPage();
                    adminPage.start(stage);
                }else {
                    showAlert("Unknown role. Please contact support.");
                }
            } else {
                showAlert("Invalid credentials.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Login error occurred.");
        }
    }

    private void showSignUpPage(Stage stage) {
        Label title = new Label("Create Account");
        title.setFont(Font.font("Arial", 24));
        title.setTextFill(Color.LIME);

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Investor", "Mentor", "Founder");
        roleBox.setPromptText("Select Role");

        Button registerBtn = new Button("Register");
        registerBtn.setStyle("-fx-background-color: #0f0; -fx-text-fill: black;");
        registerBtn.setOnAction(e -> {
            String name = nameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            String role = roleBox.getValue();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role == null) {
                showAlert("Please fill all fields.");
                return;
            }

            if (!Pattern.matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$", email)) {
                showAlert("Enter a valid Gmail address.");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement userStmt = conn.prepareStatement(
                        "INSERT INTO users (id, name, email, password, role) VALUES (users_seq.NEXTVAL, ?, ?, ?, ?)"
                );
                userStmt.setString(1, name);
                userStmt.setString(2, email);
                userStmt.setString(3, password);
                userStmt.setString(4, role);
                userStmt.executeUpdate();

                PreparedStatement getIdStmt = conn.prepareStatement("SELECT users_seq.CURRVAL FROM dual");
                ResultSet rs = getIdStmt.executeQuery();
                int userId = -1;
                if (rs.next()) {
                    userId = rs.getInt(1);
                }

                PreparedStatement roleStmt = null;
                switch (role.toLowerCase()) {
                    case "investor":
                        roleStmt = conn.prepareStatement("INSERT INTO investors (user_id, name, email, expertise_area, available_budget) VALUES (?, ?, ?, ?, ?)");
                        roleStmt.setInt(1, userId);
                        roleStmt.setString(2, name);
                        roleStmt.setString(3, email);
                        roleStmt.setString(4, "");
                        roleStmt.setDouble(5, 0.0);
                        break;

                    case "mentor":
                        roleStmt = conn.prepareStatement("INSERT INTO mentors (user_id, name, email, expertise, availability) VALUES (?, ?, ?, ?, ?)");
                        roleStmt.setInt(1, userId);
                        roleStmt.setString(2, name);
                        roleStmt.setString(3, email);
                        roleStmt.setString(4, "");
                        roleStmt.setString(5, "");
                        break;

                    case "founder":
                        roleStmt = conn.prepareStatement("INSERT INTO founders (user_id, name, email, startup_name, industry, location, team_size, funding_needed) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                        roleStmt.setInt(1, userId);
                        roleStmt.setString(2, name);
                        roleStmt.setString(3, email);
                        roleStmt.setString(4, "");
                        roleStmt.setString(5, "");
                        roleStmt.setString(6, "");
                        roleStmt.setInt(7, 1);
                        roleStmt.setDouble(8, 0.0);
                        break;

                    default:
                        showAlert("Unknown role: " + role);
                        return;
                }

                if (roleStmt != null) {
                    roleStmt.executeUpdate();
                }

                showAlert("Registration successful!");
                switch (role.toLowerCase()) {
                    case "investor": new InvestorPage(userId,name).show(stage); break;
                    case "mentor": new MentorPage(userId,name).show(stage); break;
                    case "founder": new FounderPage(userId,name).show(stage); break;
                    default:
                        showAlert("Registration successful");
                        break;
                }
                show(stage);

            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Error during registration.");
            }
        });

        Button backBtn = new Button("Back");
        backBtn.setStyle("-fx-background-color: #f0f; -fx-text-fill: black;");
        backBtn.setOnAction(e -> show(stage));

        VBox layout = new VBox(10, title, nameField, emailField, passwordField, roleBox, registerBtn, backBtn);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: black;");

        FadeTransition fade = new FadeTransition(Duration.seconds(2), layout);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        Scene scene = new Scene(layout, 400, 400);
        stage.setScene(scene);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}