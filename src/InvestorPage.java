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

public class InvestorPage {
    private final int userId;
    private final String userName;

    public InvestorPage(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public void show(Stage stage) {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color:#f0f2f5;");

        Tab profileTab = new Tab("Profile", getProfileForm(stage));
        Tab investmentTab = new Tab("Investment Requests", getInvestmentRequestsTab(stage));

        tabPane.getTabs().addAll(profileTab, investmentTab);
        tabPane.getSelectionModel().select(profileTab);

        Scene scene = new Scene(tabPane, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Investor Dashboard - " + userName);
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

        Label header = new Label("Investor Profile");
        header.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        header.setTextFill(Color.web("#34495e"));

        TextField expertiseArea = new TextField();
        TextField availableBudget = new TextField();

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM investors WHERE user_id = ?");
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                expertiseArea.setText(rs.getString("expertise_area"));
                availableBudget.setText(rs.getString("available_budget"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Button updateButton = new Button("Update Profile");
        updateButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10 20;");
        updateButton.setOnAction(e -> updateProfile(expertiseArea.getText(), availableBudget.getText()));

        // Sign out Button
        Button signOutButton = new Button("Sign Out");
        signOutButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 10 20;");
        signOutButton.setOnAction(e -> signOut(stage));

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);

        form.add(new Label("Expertise Area:"), 0, 0);
        form.add(expertiseArea, 1, 0);
        form.add(new Label("Available Budget:"), 0, 1);
        form.add(availableBudget, 1, 1);

        layout.getChildren().addAll(header, form, updateButton, signOutButton);
        return layout;
    }

    private void updateProfile(String expertiseArea, String availableBudget) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE investors SET expertise_area = ?, available_budget = ? WHERE user_id = ?");
            stmt.setString(1, expertiseArea);
            stmt.setString(2, availableBudget);
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


    private VBox getInvestmentRequestsTab(Stage stage) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label header = new Label("Investment Requests");
        header.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        header.setTextFill(Color.web("#34495e"));

        TableView<Object[]> table = createModernTable();

        TableColumn<Object[], String> nameCol = new TableColumn<>("Founder Name");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[0]));

        TableColumn<Object[], String> ideaDescCol = new TableColumn<>("Idea Description");
        ideaDescCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[1]));

        TableColumn<Object[], String> stageCol = new TableColumn<>("Stage");
        stageCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[2]));

        TableColumn<Object[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue()[3]));

        TableColumn<Object[], Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button acceptButton = new Button("Accept");
            private final Button rejectButton = new Button("Reject");

            {
                acceptButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                rejectButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");

                acceptButton.setOnAction(event -> {
                    Object[] row = getTableRow().getItem();
                    int fundingId = (Integer) row[4];
                    acceptInvestment(fundingId);
                    getTableView().getItems().remove(row);
                });

                rejectButton.setOnAction(event -> {
                    Object[] row = getTableRow().getItem();
                    int fundingId = (Integer) row[4];
                    rejectInvestment(fundingId);
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

        table.getColumns().addAll(nameCol, ideaDescCol, stageCol, statusCol, actionCol);

        // ✅ Fetch investment requests from the database
        try (Connection conn = DBConnection.getConnection()) {
            String sql =
                    "SELECT " +
                            "  a.support_name, " +                  // the founder’s name stored on the application
                            "  a.idea_desc, " +
                            "  a.stage, " +
                            "  f.amount, " +
                            "  f.funding_status, " +
                            "  f.funding_id " +                     // now included!
                            "FROM applications a " +
                            "LEFT JOIN ( " +
                            "  SELECT * FROM ( " +
                            "    SELECT " +
                            "      f.id           AS funding_id, " +
                            "      f.investor_id, " +
                            "      f.founder_id, " +
                            "      f.amount, " +
                            "      f.status       AS funding_status, " +
                            "      ROW_NUMBER() OVER ( " +
                            "        PARTITION BY f.founder_id, f.investor_id " +
                            "        ORDER BY f.id" +
                            "      ) AS rn " +
                            "    FROM funding f " +
                            "  ) sub " +
                            "  WHERE rn = 1 " +                    // pick only the most recent funding per pair
                            ") f " +
                            "  ON f.founder_id = a.founder_id " +
                            "  AND f.investor_id = a.support_id " +
                            "WHERE f.investor_id    = ? " +         // only your own requests
                            "  AND f.funding_status = 'Investment Pending' " +
                            "  AND a.status         = 'Investment Pending'";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);  // your logged‑in investor’s ID

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String founderName  = rs.getString("support_name");
                String ideaDesc     = rs.getString("idea_desc");
                String stages        = rs.getString("stage");
                String fundStatus   = rs.getString("funding_status");
                int    fundingId    = rs.getInt   ("funding_id");

                table.getItems().add(new Object[]{
                        founderName,
                        ideaDesc,
                        stages,
                        fundStatus,
                        fundingId
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        layout.getChildren().addAll(header, table);
        return layout;
    }


    private void acceptInvestment(int fundingId) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Update the status of the funding
            PreparedStatement updateFundingStmt = conn.prepareStatement(
                    "UPDATE funding SET status = 'Accepted' WHERE id = ?"
            );
            updateFundingStmt.setInt(1, fundingId);
            updateFundingStmt.executeUpdate();

            // Update the status of the application
            PreparedStatement updateAppStmt = conn.prepareStatement(
                    "UPDATE applications SET status = 'Accepted' WHERE id = ?"
            );
            updateAppStmt.setInt(1, fundingId); // Use the same ID for applications
            updateAppStmt.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void rejectInvestment(int fundingId) {
        try (Connection conn = DBConnection.getConnection()) {
            // Update the status of the funding to 'Rejected'
            PreparedStatement updateFundingStmt = conn.prepareStatement(
                    "UPDATE funding SET status = 'Rejected' WHERE id = ?"
            );
            updateFundingStmt.setInt(1, fundingId);
            updateFundingStmt.executeUpdate();

            // Update the status of the application to 'Rejected'
            PreparedStatement updateAppStmt = conn.prepareStatement(
                    "UPDATE applications SET status = 'Rejected' WHERE id = ?"
            );
            updateAppStmt.setInt(1, fundingId); // Use the same ID for applications
            updateAppStmt.executeUpdate();
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
