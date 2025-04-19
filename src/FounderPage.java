import javafx.animation.FadeTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.sql.*;

public class FounderPage {
    private final int userId;
    private final String userName;

    public FounderPage(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public void show(Stage stage) {
        // Main layout with top sign-out bar and center tabs
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#f0f2f5;");

        // Top: Sign Out button
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_RIGHT);
        Button signOutBtn = new Button("Sign Out");
        signOutBtn.setStyle("-fx-background-color:#e74c3c; -fx-text-fill:white; -fx-font-weight:bold;");
        signOutBtn.setOnAction(e -> new LoginPage().show(stage));
        topBar.getChildren().add(signOutBtn);
        root.setTop(topBar);

        // Center: TabPane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color:#f0f2f5;");

        tabPane.getTabs().addAll(
                new Tab("Profile", getProfileForm(stage)),
                new Tab("Mentors", getMentorsList()),
                new Tab("Investors", getInvestorsList()),
                new Tab("Events", getEventsList()),
                new Tab("Apply Funding", getFundingForm()),
                new Tab("Request Mentor", getMentorRequestForm())
        );
        tabPane.getSelectionModel().selectFirst();
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Founder Dashboard - " + userName);
        stage.show();

        FadeTransition ft = new FadeTransition(Duration.millis(600), tabPane);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
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

    private VBox getInvestorsList() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label header = new Label("Available Investors");
        header.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        header.setTextFill(Color.web("#34495e"));

        TableView<Object[]> table = createModernTable();

        TableColumn<Object[], String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[0]));
        nameCol.setStyle("-fx-font-weight:bold; -fx-text-fill:#2c3e50;");

        TableColumn<Object[], String> expertiseCol = new TableColumn<>("Expertise Area");
        expertiseCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[1]));

        TableColumn<Object[], String> budgetCol = new TableColumn<>("Available Budget");
        budgetCol.setCellValueFactory(data -> new ReadOnlyStringWrapper("$" + data.getValue()[2].toString()));

        table.getColumns().addAll(nameCol, expertiseCol, budgetCol);

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT u.name, i.expertise_area, i.available_budget FROM investors i JOIN users u ON i.user_id = u.id"
            );
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                table.getItems().add(new Object[]{
                        rs.getString("name"),
                        rs.getString("expertise_area"),
                        rs.getDouble("available_budget")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        layout.getChildren().addAll(header, table);
        return layout;
    }

    private VBox getMentorsList() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label header = new Label("Available Mentors");
        header.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        header.setTextFill(Color.web("#34495e"));

        TableView<Object[]> table = createModernTable();

        TableColumn<Object[], String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[0]));
        nameCol.setStyle("-fx-font-weight:bold; -fx-text-fill:#2c3e50;");

        TableColumn<Object[], String> expertiseCol = new TableColumn<>("Expertise");
        expertiseCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[1]));

        TableColumn<Object[], String> availabilityCol = new TableColumn<>("Availability");
        availabilityCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[2]));

        table.getColumns().addAll(nameCol, expertiseCol, availabilityCol);

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT u.name, m.expertise, m.availability FROM mentors m JOIN users u ON m.user_id = u.id"
            );
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                table.getItems().add(new Object[]{
                        rs.getString("name"),
                        rs.getString("expertise"),
                        rs.getString("availability")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        layout.getChildren().addAll(header, table);
        return layout;
    }

    private VBox getEventsList() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label header = new Label("Upcoming Events");
        header.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        header.setTextFill(Color.web("#34495e"));

        TableView<Object[]> table = createModernTable();

        TableColumn<Object[], String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[0]));
        titleCol.setStyle("-fx-font-weight:bold; -fx-text-fill:#2c3e50;");

        TableColumn<Object[], String> dateCol = new TableColumn<>("Event Date");
        dateCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()[1].toString()));

        TableColumn<Object[], String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[2]));

        table.getColumns().addAll(titleCol, dateCol, descCol);

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT title, event_date, description FROM events ORDER BY event_date ASC"
            );
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                table.getItems().add(new Object[]{
                        rs.getString("title"),
                        rs.getDate("event_date"),
                        rs.getString("description")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        layout.getChildren().addAll(header, table);
        return layout;
    }

    private VBox getProfileForm(Stage stage) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_LEFT);

        Label header = new Label("Your Profile");
        header.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        header.setTextFill(Color.web("#34495e"));

        TextField startupName = new TextField();
        TextField industry = new TextField();
        TextField location = new TextField();
        TextField teamSize = new TextField();
        TextField fundingNeeded = new TextField();

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM founders WHERE user_id = ?");
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                startupName.setText(rs.getString("startup_name"));
                industry.setText(rs.getString("industry"));
                location.setText(rs.getString("location"));
                teamSize.setText(rs.getString("team_size"));
                fundingNeeded.setText(rs.getString("funding_needed"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);

        form.add(new Label("Startup Name:"), 0, 0);
        form.add(startupName, 1, 0);
        form.add(new Label("Industry:"), 0, 1);
        form.add(industry, 1, 1);
        form.add(new Label("Location:"), 0, 2);
        form.add(location, 1, 2);
        form.add(new Label("Team Size:"), 0, 3);
        form.add(teamSize, 1, 3);
        form.add(new Label("Funding Needed:"), 0, 4);
        form.add(fundingNeeded, 1, 4);

        layout.getChildren().addAll(header, form);
        return layout;
    }

    private VBox getFundingForm() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label header = new Label("Apply for Funding");
        header.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        header.setTextFill(Color.web("#34495e"));

        TextField amountField = new TextField();
        amountField.setPromptText("Enter Funding Amount");

        TextArea ideaField = new TextArea();
        ideaField.setPromptText("Describe your startup idea");
        ideaField.setWrapText(true);

        ComboBox<String> stageBox = new ComboBox<>();
        stageBox.getItems().addAll("Ideation", "MVP", "Scaling");
        stageBox.setPromptText("Select Startup Stage");

        // ComboBox to choose Investor
        ComboBox<String> investorCombo = new ComboBox<>();
        investorCombo.setPromptText("Select Investor");
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.name FROM investors i JOIN users u ON i.user_id=u.id"
            );
            ResultSet rs=ps.executeQuery();
            while(rs.next()) investorCombo.getItems().add(rs.getString(1));
        } catch(SQLException ex){ex.printStackTrace();}

        Button submitBtn = new Button("Submit Funding Request");
        submitBtn.setStyle("-fx-background-color:#3498db; -fx-text-fill:white; -fx-font-weight:bold;");
        submitBtn.setOnAction(e -> {
            String amt=amountField.getText().trim();
            String idea=ideaField.getText().trim();
            String stg=stageBox.getValue();
            String invName=investorCombo.getValue();
            if(amt.isEmpty()||idea.isEmpty()||stg==null||invName==null) {
                showAlert("Error", "Fill all fields!", Alert.AlertType.ERROR);return;
            }
            double a;
            try{a=Double.parseDouble(amt);}catch(Exception ex){showAlert("Error", "Invalid amount", Alert.AlertType.ERROR);return;}
            int invId=-1;
            try(Connection c=DBConnection.getConnection()){
                PreparedStatement p=c.prepareStatement(
                        "SELECT user_id FROM investors i JOIN users u ON i.user_id=u.id WHERE u.name=?"
                );p.setString(1,invName);
                ResultSet r=p.executeQuery(); if(r.next()) invId=r.getInt(1);
            }catch(Exception ex){ex.printStackTrace();}
            if(invId<0){showAlert("Error", "Investor not found", Alert.AlertType.ERROR);return;}
            try (Connection c = DBConnection.getConnection()) {
                // Insert into funding table
                PreparedStatement f = c.prepareStatement(
                        "INSERT INTO funding(id, investor_id, founder_id, amount, status) " +
                                "VALUES(funding_seq.NEXTVAL, ?, ?, ?, 'Investment Pending')"
                );
                f.setInt(1, invId);
                f.setInt(2, userId);
                f.setDouble(3, a);
                f.executeUpdate();

                // Insert into applications table with pair_id only
                PreparedStatement ap = c.prepareStatement(
                        "INSERT INTO applications(id, founder_id, idea_desc, stage, status, support_id,support_name ) " +
                                "VALUES(applications_seq.NEXTVAL, ?, ?, ?, 'Investment Pending', ?,?)"
                );
                ap.setInt(1, userId);
                ap.setString(2, idea);
                ap.setString(3, stg);
                ap.setInt(4, invId); // pair_id as investor's user_id
                ap.setString(5, userName);
                ap.executeUpdate();

                showAlert("Success", "Funding + Application submitted.", Alert.AlertType.INFORMATION);
                amountField.clear(); ideaField.clear(); stageBox.getSelectionModel().clearSelection(); investorCombo.getSelectionModel().clearSelection();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Error", "Submission failed.", Alert.AlertType.ERROR);
            }

        });

        layout.getChildren().addAll(header, investorCombo, amountField, ideaField, stageBox, submitBtn);
        return layout;
    }
    private VBox getMentorRequestForm() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_LEFT);

        Label header = new Label("Request Mentor");
        header.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        header.setTextFill(Color.web("#34495e"));

        // Mentor Request Form Fields
        ComboBox<String> mentorComboBox = new ComboBox<>();
        TextArea ideaDescription = new TextArea();
        ComboBox<String> stageComboBox = new ComboBox<>();
        Button submitButton = new Button("Submit Request");

        // Setting stage options
        stageComboBox.getItems().addAll("Ideation", "MVP", "Scaling");

        // Get available mentors for the ComboBox
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT u.name FROM mentors m JOIN users u ON m.user_id = u.id");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mentorComboBox.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        submitButton.setOnAction(event -> {
            String selectedMentor = mentorComboBox.getValue();
            String description = ideaDescription.getText();
            String stage = stageComboBox.getValue();

            if (selectedMentor == null || description.isEmpty() || stage == null) {
                showAlert("Error", "All fields are required.", Alert.AlertType.ERROR);
                return;
            }

            // Get mentor ID from the selected mentor name
            int mentorId = getMentorIdFromName(selectedMentor);

            // Insert into mentor_requests table
            try (Connection conn = DBConnection.getConnection()) {
                // Insert into mentor_requests
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO mentor_requests (mentor_id, founder_id, status) VALUES (?, ?, ?)"
                );
                stmt.setInt(1, mentorId);
                stmt.setInt(2, userId);
                stmt.setString(3, "Mentor Pending");
                stmt.executeUpdate();

                // Insert into applications with only pair_id
                stmt = conn.prepareStatement(
                        "INSERT INTO applications (id, founder_id, idea_desc, stage, status, support_id,support_name) " +
                                "VALUES (applications_seq.NEXTVAL, ?, ?, ?, ?, ?,?)"
                );
                stmt.setInt(1, userId);
                stmt.setString(2, description);
                stmt.setString(3, stage);
                stmt.setString(4, "Mentor Pending");
                stmt.setInt(5, mentorId); // pair_id = mentor's user_id
                stmt.setString(6, userName); // support_name = mentor name

                stmt.executeUpdate();

                showAlert("Success", "Mentor request submitted successfully.", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to submit the mentor request.", Alert.AlertType.ERROR);
            }

        });

        layout.getChildren().addAll(header, new Label("Select Mentor:"), mentorComboBox,
                new Label("Describe Your Idea:"), ideaDescription,
                new Label("Stage of Your Startup:"), stageComboBox, submitButton);

        return layout;
    }

    private int getMentorIdFromName(String mentorName) {
        int mentorId = -1;
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT m.user_id FROM mentors m JOIN users u ON m.user_id = u.id WHERE u.name = ?"
            );
            stmt.setString(1, mentorName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                mentorId = rs.getInt("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mentorId;
    }
    private void showAlert(String s, String msg, Alert.AlertType error) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
