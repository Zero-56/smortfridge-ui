package ee2.smortfridge;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
//this guy will get rid of the problem of having spaces in the names for http urls
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
//this guy is for the pichart view for lazy susan
import javafx.scene.chart.PieChart;

public class UI extends Application {
    private Connector connector;

    // ── Style helpers ─────────────────────────────────────────────────────────

    /** Bold blue column header label */
    private Label headerLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("grid-header");
        return l;
    }

    /** Apply urgency colour class to a "days left" label */
    private void applyUrgencyStyle(Label label, int days) {
        label.getStyleClass().removeAll("urgency-ok", "urgency-warn", "urgency-expired");
        if (days < 0) {
            label.getStyleClass().add("urgency-expired");
        } else if (days <= 2) {
            label.getStyleClass().add("urgency-warn");
        } else {
            label.getStyleClass().add("urgency-ok");
        }
    }

    /** Apply the shared stylesheet to any pop-up scene */
    private void applyStylesheet(Scene scene) {
        java.net.URL cssUrl = getClass().getResource("/ee2/smortfridge/StylishFridge.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Original methods below — HTTP logic, try-catch blocks, and control flow
    // are preserved exactly as they were. Only styling lines have been changed.
    // ─────────────────────────────────────────────────────────────────────────

    // We make this a method so we can call it whenever we want to update the UI
    public void refreshTable(GridPane inventoryGrid, Stage primaryStage) {
        HttpClient client = HttpClient.newHttpClient();
        // Use your specific pantry endpoint
        String url = "https://studev.groept.be/api/a25ee2team206/PantryScene";

        HttpResponse<String> response = connector.makeGETRequest(client, url);
        if (response != null && response.body() != null) {
            // We use the logic you already have
            parsePantryScene(response.body(), inventoryGrid, primaryStage);
        }
    }

    //dropdown for the storages:
    private void populateStorageBox(ComboBox<String> box) {
        HttpClient client = HttpClient.newHttpClient();
        // Assuming your endpoint for getting storage list is /getStorage
        String url = "https://studev.groept.be/api/a25ee2team206/getStorage";

        try {
            HttpResponse<String> response = connector.makeGETRequest(client, url);
            if (response != null && response.body() != null) {
                JSONArray array = new JSONArray(response.body());

                box.getItems().clear();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    box.getItems().add(o.getString("name"));
                }

                box.getItems().add("--- Add New Storage ---");
                box.setOnAction(e -> {
                    if ("--- Add New Storage ---".equals(box.getValue())) {
                        box.getSelectionModel().clearSelection();
                        openManageStorageWindow(); // Reuses your existing manager
                        // After returning, refresh the box
                        populateStorageBox(box);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Could not load storages: " + e.getMessage());
        }
    }

    //fuction for adding new categpries to the DB
    private void openAddCategoryWindow(ComboBox<String> categoryBox) {
        Stage catStage = new Stage();
        catStage.setTitle("Add New Category");

        TextField catNameField = new TextField();
        catNameField.setPromptText("Category name...");
        TextField safeTimeField = new TextField();
        safeTimeField.setPromptText("Days of freshness");
        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("button-add");

        saveBtn.setOnAction(e -> {
            String newName = catNameField.getText();
            //no problem of having a space here! :3
            newName = URLEncoder.encode(newName, StandardCharsets.UTF_8);
            String newSafeTime = safeTimeField.getText();
            if (!newName.isBlank()) {
                HttpClient client = HttpClient.newHttpClient();
                String url = "https://studev.groept.be/api/a25ee2team206/addCategory/"
                        + newName + "/"
                        + newSafeTime;

                // Execute the call
                connector.makeGETRequest(client, url);
                // Refresh the dropdown
                populateCategoryBox(categoryBox);
                catStage.close();
            }
        });

        Label title = new Label("Add New Category");
        title.getStyleClass().add("section-title");

        // Layout (GridPane or VBox)
        VBox layout = new VBox(10, title, new Label("Category Name:"), catNameField, new Label("Days of Freshness:"), safeTimeField, saveBtn);
        layout.setPadding(new Insets(20));
        Scene scene = new Scene(layout, 280, 220);
        applyStylesheet(scene);
        catStage.setScene(scene);
        catStage.show();
    }

    //the category manager scene
    private void openManageCategoriesWindow(ComboBox<String> categoryBox) {
        Stage manageStage = new Stage();
        manageStage.setTitle("Manage Categories");

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        VBox list = new VBox(10);
        list.setPadding(new Insets(20));

        Label warningLabel = new Label("");
        warningLabel.getStyleClass().add("label-warn");
        warningLabel.setMinHeight(30); // Force the label to take up space
        warningLabel.setWrapText(true);

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> manageStage.close());

        ScrollPane scroll = new ScrollPane(list);
        scroll.setPrefViewportHeight(300);

        try {
            HttpClient client = HttpClient.newHttpClient();
            String url = "https://studev.groept.be/api/a25ee2team206/categoryManagerScene";
            HttpResponse<String> response = connector.makeGETRequest(client, url);

            if (response != null && response.body() != null) {
                JSONArray array = new JSONArray(response.body());

                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    String rawCatName = o.getString("name"); // KEEP RAW
                    int safeTime = o.getInt("safeStorageTime");

                    Button delBtn = new Button("✕");
                    delBtn.getStyleClass().addAll("button-delete", "button-x");

                    delBtn.setOnAction(e -> {
                        // Encode ONLY here
                        String encodedName = URLEncoder.encode(rawCatName, StandardCharsets.UTF_8);
                        String delUrl = "https://studev.groept.be/api/a25ee2team206/deleteCategory/" + encodedName;

                        HttpResponse<String> delResponse = connector.makeGETRequest(HttpClient.newHttpClient(), delUrl);
                        //an overengineered if to determine if we get a forgein key fatal error to tell the user that you cant delete a category currently in use ToT
                        if (delResponse != null && delResponse.body() != null && delResponse.body().contains("constraint")) {
                            warningLabel.setText("Can't delete: Category is in use!");
                            System.out.println("cat delete i use");
                        } else {
                            manageStage.close();
                            openManageCategoriesWindow(categoryBox);
                            populateCategoryBox(categoryBox);
                        }
                    });

                    // Use the raw name for the display
                    Label infoLabel = new Label(rawCatName + " (Fresh for: " + safeTime + " days)");
                    list.getChildren().add(new javafx.scene.layout.HBox(10, infoLabel, delBtn));
                }
            }
        } catch (Exception e) {
            list.getChildren().add(new Label("Error: " + e.getMessage()));
        }

        Label title = new Label("Manage Categories:");
        title.getStyleClass().add("section-title");

        root.getChildren().addAll(title, scroll, warningLabel, closeBtn);

        Scene scene = new Scene(root, 350, 450);
        applyStylesheet(scene);
        manageStage.setScene(scene);
        manageStage.show();
    }

    //helper function for the category dropdowns
    private void populateCategoryBox(ComboBox<String> box) {
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://studev.groept.be/api/a25ee2team206/getCategories";

        try {
            HttpResponse<String> response = connector.makeGETRequest(client, url);
            if (response != null && response.body() != null) {
                JSONArray array = new JSONArray(response.body());

                box.getItems().clear(); // Clear default items
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    box.getItems().add(o.getString("name"));
                }

                //adding the "add category" option at the end of the drop down
                box.getItems().add("--- Add New Category ---");
                //listener that activates the add categpry window
                box.setOnAction(e -> {
                    if ("--- Add New Category ---".equals(box.getValue())) {
                        box.getSelectionModel().clearSelection(); // Reset so it doesn't stay selected
                        openAddCategoryWindow(box); // Open the window to add a new one
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Could not load categories: " + e.getMessage());
        }
    }

    //delete method for pantry scene
    public void deleteFood(String foodName, Stage currentStage, Runnable refreshAction) {
        // 1. Show Confirmation
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Item");
        alert.setHeaderText("Are you sure you want to delete " + foodName + "?");

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                // 2. Perform URL Call
                HttpClient client = HttpClient.newHttpClient();
                String url = "https://studev.groept.be/api/a25ee2team206/deleteFood/" + URLEncoder.encode(foodName, StandardCharsets.UTF_8);

                connector.makeGETRequest(client, url);

                // 3. Refresh UI took it our bc it doesnt work with by-category/storage views, will call the refresh table manually in each method on delete instead
                //refreshTable(inventoryGrid, currentStage);
                if (refreshAction != null) {
                    refreshAction.run();
                }
            }
        });
    }

    //the parser and scene builder for the main screen for 1 specific storage space
    public void parsePantryScene(String jsonString, GridPane targetGrid, Stage currentStage) {
        try {
            JSONArray array = new JSONArray(jsonString);

            targetGrid.getChildren().removeIf(node -> {
                Integer rowIndex = GridPane.getRowIndex(node);
                return rowIndex != null && rowIndex > 0;
            });

            for (int i = 0; i < array.length(); i++) {
                JSONObject rowData = array.getJSONObject(i);

                String name = rowData.optString("name", "Unknown");
                String amount = rowData.optString("amount", "0");
                String unit = rowData.isNull("unit") ? "" : rowData.getString("unit");
                int daysLeft = rowData.optInt("days_left", 0);

                Label nameLabel = new Label(name);
                Label amountLabel = new Label(amount);
                Label unitLabel = new Label(unit);
                Label daysLabel = new Label(String.valueOf(daysLeft));
                applyUrgencyStyle(daysLabel, daysLeft);

                //adding the edit button for each item
                Button rowEditButton = new Button("Edit");
                rowEditButton.setOnAction(e -> showEditFoodWindow(currentStage, targetGrid, name));

                //adding the delete button for each item
                Button deleteBtn = new Button("Delete");
                deleteBtn.getStyleClass().add("button-delete");
                deleteBtn.setOnAction(e -> deleteFood(name, currentStage,() -> refreshTable(targetGrid,currentStage) ));

                int row = i + 1;
                targetGrid.add(nameLabel, 0, row);
                targetGrid.add(amountLabel, 1, row);
                targetGrid.add(unitLabel, 2, row);
                targetGrid.add(daysLabel, 3, row);
                targetGrid.add(rowEditButton, 4, row);
                targetGrid.add(deleteBtn, 5, row);
            }

            currentStage.sizeToScene();

        } catch (Exception e) {
            System.err.println("Error parsing pantry data: " + e.getMessage());
        }
        currentStage.sizeToScene();
    }

    // Adds the food to the database with required fields that are NonNull in the DB
    // UPDATED: Added defaultCategory parameter
    public void showAddFoodWindow(Stage ownerStage, Node refreshTarget, String defaultCategory, String defaultStorage) {
        Stage addStage = new Stage();
        addStage.setTitle("Add New Food Item");

        GridPane form = new GridPane();
        form.setPadding(new Insets(20));
        form.setHgap(10);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);

        ComboBox<String> categoryInput = new ComboBox<>();
        populateCategoryBox(categoryInput);

        // Pre-select if a category was passed
        if (defaultCategory != null && !defaultCategory.equals("Uncategorized")) {
            categoryInput.setValue(defaultCategory);
        }

        Button manageBtn = new Button("Manage");
        manageBtn.setPrefWidth(80);
        manageBtn.setOnAction(e -> openManageCategoriesWindow(categoryInput));

        TextField nameInput = new TextField();
        nameInput.setPromptText("e.g. Milk");
        TextField amountInput = new TextField();
        amountInput.setPromptText("e.g. 2");
        TextField unitInput = new TextField();
        unitInput.setPromptText("e.g. L");

        //dropdown for storage
        ComboBox<String> storageInput = new ComboBox<>();
        populateStorageBox(storageInput);

        //preselecting the storage if given
        if (defaultStorage != null) {
            storageInput.setValue(defaultStorage);
        }
        javafx.scene.control.DatePicker dateInput = new javafx.scene.control.DatePicker();
        dateInput.setValue(java.time.LocalDate.now());

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("label-error");

        form.add(new Label("Food Name (*):"), 0, 0);
        form.add(nameInput, 1, 0);
        form.add(new Label("Category (*):"), 0, 1);
        form.add(categoryInput, 1, 1);
        form.add(manageBtn, 2, 1);
        form.add(new Label("Date of Input (*):"), 0, 2);
        form.add(dateInput, 1, 2);
        form.add(new Label("Amount:"), 0, 3);
        form.add(amountInput, 1, 3);
        form.add(new Label("Unit:"), 0, 4);
        form.add(unitInput, 1, 4);
        form.add(new Label("Storage (*):"), 0, 5);
        form.add(storageInput, 1, 5);
        form.add(errorLabel, 1, 6);

        Button saveBtn = new Button("Confirm Add");
        saveBtn.getStyleClass().add("button-add");

        saveBtn.setOnAction(event -> {
            if (nameInput.getText().isBlank() || dateInput.getValue() == null || storageInput.getValue().isBlank() || categoryInput.getValue().isBlank()) {
                errorLabel.setText("Name, Date, and Storage are required!");
                return;
            }

            String cat = categoryInput.getValue() != null ? categoryInput.getValue() : "";
            String encodedCat = URLEncoder.encode(cat, StandardCharsets.UTF_8);
            String newName = URLEncoder.encode(nameInput.getText(), StandardCharsets.UTF_8);

            String url = "https://studev.groept.be/api/a25ee2team206/addFood/"
                    + newName + "/"
                    + encodedCat + "/"
                    + dateInput.getValue().toString() + "/"
                    + URLEncoder.encode(amountInput.getText(), StandardCharsets.UTF_8)+ "/"
                    + unitInput.getText() + "/"
                    + URLEncoder.encode(storageInput.getValue(), StandardCharsets.UTF_8);

            connector.makeGETRequest(HttpClient.newHttpClient(), url);

            // Since we are in a sub-view, we might need a general refresh
            // For now, let's just close the window.
            // Tip: You can trigger a view swap to refresh the main screen!
            addStage.close();
        });

        form.add(saveBtn, 1, 7);
        Scene scene = new Scene(form, 400, 400);
        applyStylesheet(scene);
        addStage.setScene(scene);
        addStage.show();
    }

    //the window for editing food, you are allowed to leave the fields you dont want to edit empty
    public void showEditFoodWindow(Stage ownerStage, GridPane inventoryGrid, String oldName) {
        Stage editStage = new Stage();
        editStage.setTitle("Edit: " + oldName);

        GridPane form = new GridPane();
        form.setPadding(new Insets(20));
        form.setHgap(10);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);

        TextField nameInput = new TextField();
        //adding the dropdown menu for teh categories
        ComboBox<String> categoryInput = new ComboBox<>();
        populateCategoryBox(categoryInput);
        //manage categories button
        Button manageBtn = new Button("Manage");
        manageBtn.setPrefWidth(80);
        manageBtn.setOnAction(e -> openManageCategoriesWindow(categoryInput));

        TextField amountInput = new TextField();
        TextField unitInput = new TextField();
        //dropdown for stoaregs
        ComboBox<String> storageInput = new ComboBox<>();
        populateStorageBox(storageInput);
        javafx.scene.control.DatePicker dateInput = new javafx.scene.control.DatePicker();

        form.add(new Label("New Name:"), 0, 0);
        form.add(nameInput, 1, 0);
        form.add(new Label("Category:"), 0, 1);
        form.add(categoryInput, 1, 1);
        form.add(manageBtn, 2, 1);
        form.add(new Label("Date of Input:"), 0, 2);
        form.add(dateInput, 1, 2);
        form.add(new Label("Amount:"), 0, 3);
        form.add(amountInput, 1, 3);
        form.add(new Label("Unit:"), 0, 4);
        form.add(unitInput, 1, 4);
        form.add(new Label("Storage:"), 0, 5);
        form.add(storageInput, 1, 5);

        Button updateBtn = new Button("Update Food");
        updateBtn.getStyleClass().add("button-add");
        updateBtn.setOnAction(event -> {
            // We handle the Date carefully since .toString() on a null value crashes
            String dateStr = (dateInput.getValue() == null) ? "" : dateInput.getValue().toString();

            // this gives us the current sleected category
            String cat = categoryInput.getValue() != null ? categoryInput.getValue() : "";
            //ensuring we can use spaces in the name
            String newName = URLEncoder.encode(nameInput.getText(), StandardCharsets.UTF_8);
            String url = "https://studev.groept.be/api/a25ee2team206/editFood/"
                    + newName + "/"
                    + cat + "/"
                    + dateStr + "/"
                    + amountInput.getText() + "/"
                    + unitInput.getText() + "/"
                    + URLEncoder.encode(storageInput.getValue(), StandardCharsets.UTF_8) + "/"
                    + URLEncoder.encode(oldName, StandardCharsets.UTF_8);

            connector.makeGETRequest(HttpClient.newHttpClient(), url);
            refreshTable(inventoryGrid, ownerStage);
            editStage.close();
        });

        form.add(updateBtn, 1, 6);
        Scene scene = new Scene(form, 400, 350);
        applyStylesheet(scene);
        editStage.setScene(scene);
        editStage.show();
    }

    //the all food view in the main menu
    private javafx.scene.Node buildAllFoodGrid(Stage stage) {
        // 1. Create the Main Layout Container
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(10));
        mainContainer.setAlignment(Pos.TOP_CENTER);

        // 2. Create the Refresh Button
        Button refreshBtn = new Button("🔄 Refresh Table");
        refreshBtn.getStyleClass().add("button-add"); // Using your Teal color class

        // 3. Create the Grid
        GridPane inventoryGrid = new GridPane();
        inventoryGrid.setHgap(20);
        inventoryGrid.setVgap(10);
        inventoryGrid.setAlignment(Pos.CENTER);

        // 4. Add Headers (using a helper or standard Labels)
        inventoryGrid.add(new Label("Name"), 0, 0);
        inventoryGrid.add(new Label("Amount"), 1, 0);
        inventoryGrid.add(new Label("Unit"), 2, 0);
        inventoryGrid.add(new Label("Days Left"), 3, 0);
        inventoryGrid.add(new Label("Actions"), 4, 0, 2, 1);

        // 5. Setup Refresh Logic
        refreshBtn.setOnAction(e -> refreshTable(inventoryGrid, stage));

        // 6. Initial Fetch
        refreshTable(inventoryGrid, stage);

        // 7. Wrap Grid in ScrollPane
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(inventoryGrid);
        scroll.setFitToWidth(true);
        scroll.setPadding(new Insets(10));

        // 8. Assemble: Button at the top, Scrollable list below
        mainContainer.getChildren().addAll(refreshBtn, scroll);

        return mainContainer;
    }

    //Fridge Info view for temp, humidity and snack weight
    private javafx.scene.Node buildFridgeInfoView(Stage stage) {
        // Main container for the view
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);

        // Refresh Button: Re-calls this method to swap the center with fresh data
        Button refreshBtn = new Button("🔄 Refresh Sensor Data");
        refreshBtn.getStyleClass().add("button-add");
        refreshBtn.setOnAction(e -> {
            Scene scene = stage.getScene();
            if (scene != null && scene.getRoot() instanceof BorderPane) {
                BorderPane mainLayout = (BorderPane) scene.getRoot();
                mainLayout.setCenter(buildFridgeInfoView(stage));
            }
        });

        // The Info Card: Styled via your CSS "header-box"
        VBox infoCard = new VBox(15);
        infoCard.setPadding(new Insets(20));
        infoCard.getStyleClass().add("header-box");
        infoCard.setMaxWidth(400);

        // --- FETCH DATA FROM SEPARATE URLs ---
        double temp = fetchSensorValue("https://studev.groept.be/api/a25ee2team206/getLastTemperature", "Temperature");
        double humidity = fetchSensorValue("https://studev.groept.be/api/a25ee2team206/getLastHumidity", "Humidity");
        double weight = fetchSensorValue("https://studev.groept.be/api/a25ee2team206/getLastWeight", "Weight");

        //avoiding negative weight
        if(weight<0){
            weight = 0;
        }

        // Title for the card
        Label title = new Label("LIVE FRIDGE STATUS");
        title.getStyleClass().add("header-label");
        title.setStyle("-fx-font-size: 18px; -fx-text-fill: #1a2a3a;");

        // Add the rows to the card
        infoCard.getChildren().addAll(
                title,
                new Separator(),
                createSensorRow("🌡 Temperature:", temp + " °C"),
                createSensorRow("💧 Humidity:", humidity + " %"),
                createSensorRow("⚖️ Snack Weight:", weight + " g")
        );

        root.getChildren().addAll(refreshBtn, infoCard);
        return root;
    }

    //helpers for fetching sensor info
    /**
     * Helper to fetch a single double value from a specific URL and JSON key
     */
    private double fetchSensorValue(String url, String key) {
        try {
            HttpResponse<String> response = connector.makeGETRequest(HttpClient.newHttpClient(), url);
            if (response != null && response.body() != null) {
                JSONArray array = new JSONArray(response.body());
                if (array.length() > 0) {
                    // Returns the value for the specific key (e.g., "Temperature")
                    return array.getJSONObject(0).optDouble(key, 0.0);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching " + key + ": " + e.getMessage());
        }
        return 0.0; // Fallback if sensor is offline or URL fails
    }

    /**
     * Helper to create a nice-looking row for the sensor display
     */
    private HBox createSensorRow(String labelText, String valueText) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-weight: bold; -fx-min-width: 150px;");

        Label val = new Label(valueText);
        val.setStyle("-fx-text-fill: #5477b5; -fx-font-family: 'Monospaced'; -fx-font-size: 14px;");

        HBox row = new HBox(10, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // access time menu
    private javafx.scene.Node buildAccessTimeView(Stage stage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        // --- ADD SECTION ---
        Label addLabel = new Label("Define a New Diet Access Period:");
        addLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        TextField nameInput = new TextField();
        nameInput.setPromptText("Name (e.g., Midnight Snack)");

        TextField startInput = new TextField();
        startInput.setPromptText("Start (hh:mm:ss)");
        startInput.setPrefWidth(100);

        TextField endInput = new TextField();
        endInput.setPromptText("End (hh:mm:ss)");
        endInput.setPrefWidth(100);

        Button addBtn = new Button("Add Period");
        addBtn.getStyleClass().add("button-add");

        VBox listContainer = new VBox(10);
        listContainer.setPadding(new Insets(10));

        addBtn.setOnAction(e -> {
            String name = nameInput.getText().trim();
            String start = startInput.getText().trim();
            String end = endInput.getText().trim();

            if (!name.isEmpty() && !start.isEmpty() && !end.isEmpty()) {
                String encName = URLEncoder.encode(name, StandardCharsets.UTF_8);
                String encStart = URLEncoder.encode(start, StandardCharsets.UTF_8);
                String encEnd = URLEncoder.encode(end, StandardCharsets.UTF_8);

                // Assuming your add API takes Name/Start/End
                String addUrl = "https://studev.groept.be/api/a25ee2team206/addAccessTime/" + encName + "/" + encStart + "/" + encEnd;
                connector.makeGETRequest(HttpClient.newHttpClient(), addUrl);

                nameInput.clear(); startInput.clear(); endInput.clear();
                refreshAccessTimes(listContainer, stage);
            }
        });

        HBox inputRow = new HBox(10, nameInput, startInput, new Label("to"), endInput, addBtn);
        inputRow.setAlignment(Pos.CENTER);

        // --- LIST SECTION ---
        refreshAccessTimes(listContainer, stage);

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(400);
        scroll.setStyle("-fx-background-color: transparent;");

        root.getChildren().addAll(addLabel, inputRow, new Separator(), scroll);
        return root;
    }

    //The refresh method to fetch and display the data for access times
    private void refreshAccessTimes(VBox listContainer, Stage stage) {
        listContainer.getChildren().clear();
        String url = "https://studev.groept.be/api/a25ee2team206/getAccessTime";

        HttpResponse<String> response = connector.makeGETRequest(HttpClient.newHttpClient(), url);
        if (response != null && response.body() != null) {
            JSONArray array = new JSONArray(response.body());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                // Matches your JSON keys: DietTime, accessStart, accessEnd
                String name = obj.optString("DietTime", "Unnamed Period");
                String start = obj.optString("accessStart", "00:00:00");
                String end = obj.optString("accessEnd", "00:00:00");

                // UI Label for the period
                Label nameLabel = new Label(name);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 180px;");

                Label timeLabel = new Label(start + " - " + end);
                timeLabel.setStyle("-fx-text-fill: #5477b5; -fx-min-width: 150px;");

                Button delBtn = new Button("Delete");
                delBtn.getStyleClass().add("button-delete");

                delBtn.setOnAction(e -> {
                    // Using DietTime as the identifier for deletion
                    String encName = URLEncoder.encode(name, StandardCharsets.UTF_8);
                    String delUrl = "https://studev.groept.be/api/a25ee2team206/deleteAccessTime/" + encName;
                    //IO.println(delUrl);
                    connector.makeGETRequest(HttpClient.newHttpClient(), delUrl);
                    refreshAccessTimes(listContainer, stage);
                });

                HBox row = new HBox(15, nameLabel, timeLabel, delBtn);
                row.setAlignment(Pos.CENTER);
                row.setPadding(new Insets(10));
                row.getStyleClass().add("header-box"); // Reusing your card style

                listContainer.getChildren().add(row);
            }
        } else {
            listContainer.getChildren().add(new Label("No access times found in the database."));
        }
    }

    //Lazy susan pichart view
    private javafx.scene.Node buildLazySusanView(Stage stage, BorderPane mainLayout) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);

        //chose a color palette different from the app cos whgy not
        final String[] palette = new String[]{
                "#e07b54",  // terracotta
                "#e8b84b",  // mustard
                "#6dbf8b",  // sage green
                "#5ba4a4",  // muted teal
                "#7b8fd4",  // periwinkle
                "#c26dbd",  // mauve
                "#e06b8b",  // dusty rose
                "#7dc4c4"   // seafoam
        };

        Label title = new Label("Lazy Susan Control");
        title.getStyleClass().add("section-title");
        Label hint = new Label("Click to set to front  •  Double-click to edit name");
        hint.setStyle("-fx-text-fill: grey; -fx-font-size: 11px;");


        PieChart lazySusanChart = new PieChart();
        lazySusanChart.setLegendVisible(false);
        lazySusanChart.setLabelsVisible(true);
        lazySusanChart.setAnimated(false);

        // --- 1. Get Current Front ID ---
        int currentFrontId = -1;
        String frontUrl = "https://studev.groept.be/api/a25ee2team206/getCurrentFrontLazySusan";
        HttpResponse<String> frontResponse = connector.makeGETRequest(HttpClient.newHttpClient(), frontUrl);
        if (frontResponse != null && frontResponse.body() != null) {
            JSONArray frontArr = new JSONArray(frontResponse.body());
            if (frontArr.length() > 0) {
                currentFrontId = frontArr.getJSONObject(0).optInt("segment", -1);
            }
        }
        final int frontId = currentFrontId;

        // --- 2. Get Segment Data ---
        // Store raw food names and segment IDs in parallel lists, indexed by slice order
        java.util.List<String> rawFoodNames = new java.util.ArrayList<>();
        java.util.List<Integer> segmentIds = new java.util.ArrayList<>();

        String infoUrl = "https://studev.groept.be/api/a25ee2team206/getLazySusanInfo";
        HttpResponse<String> infoResponse = connector.makeGETRequest(HttpClient.newHttpClient(), infoUrl);
        if (infoResponse != null && infoResponse.body() != null) {
            JSONArray items = new JSONArray(infoResponse.body());
            for (int i = 0; i < items.length(); i++) {
                JSONObject obj = items.getJSONObject(i);
                int segmentId = obj.optInt("segment");
                String foodName = obj.optString("Food", "Empty");

                rawFoodNames.add(foodName);
                segmentIds.add(segmentId);

                // Label shows segment number + food name, but raw name is stored separately
                lazySusanChart.getData().add(new PieChart.Data("[" + segmentId + "] " + foodName, 1.0));
            }
        }

        // --- 3. Apply styles after layout ---
        lazySusanChart.widthProperty().addListener(new javafx.beans.value.ChangeListener<Number>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Number> obs, Number oldVal, Number newVal) {
                lazySusanChart.widthProperty().removeListener(this);
                javafx.application.Platform.runLater(() -> {
                    java.util.List<PieChart.Data> data = lazySusanChart.getData();
                    for (int i = 0; i < data.size(); i++) {
                        javafx.scene.Node node = data.get(i).getNode();
                        if (node == null) continue;

                        // Use the parallel lists for the correct raw name and segment ID
                        final String rawFood = rawFoodNames.get(i);
                        final int segId = segmentIds.get(i);
                        boolean isFront = (segId == frontId);

                        node.setStyle("-fx-pie-color: " + palette[i % palette.length] + ";"
                                + (isFront ? "-fx-effect: dropshadow(gaussian, white, 15, 0.6, 0, 0);" : ""));
                        if (isFront) { node.setScaleX(1.12); node.setScaleY(1.12); }

                        node.setCursor(javafx.scene.Cursor.HAND);
                        node.setOnMouseClicked(e -> {
                            if (e.getClickCount() == 2) {
                                // Double-click: edit the food name
                                TextInputDialog dialog = new TextInputDialog(rawFood);
                                dialog.setTitle("Edit Segment " + segId);
                                dialog.setHeaderText(null);
                                dialog.setContentText("New food name:");
                                dialog.showAndWait().ifPresent(newName -> {
                                    if (!newName.isBlank()) {
                                        connector.makeGETRequest(HttpClient.newHttpClient(),
                                                "https://studev.groept.be/api/a25ee2team206/changeLazySusanSegmentCategory/"
                                                        + URLEncoder.encode(newName, java.nio.charset.StandardCharsets.UTF_8)
                                                        + "/" + segId);
                                        mainLayout.setCenter(buildLazySusanView(stage, mainLayout));
                                    }
                                });
                            } else if (e.getClickCount() == 1) {
                                // Delay single-click so a double-click can cancel it
                                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));
                                pause.setOnFinished(ev -> {
                                    connector.makeGETRequest(HttpClient.newHttpClient(),
                                            "https://studev.groept.be/api/a25ee2team206/addLazySusanActivationLog/"
                                                    + URLEncoder.encode(rawFood, java.nio.charset.StandardCharsets.UTF_8));
                                    mainLayout.setCenter(buildLazySusanView(stage, mainLayout));
                                });
                                pause.play();
                                // If a second click arrives within 250ms, the double-click branch above
                                // fires and this pause just finishes harmlessly after the view rebuilds
                            }
                        });
                    }
                });
            }
        });

        root.getChildren().addAll(title, hint, lazySusanChart);
        return root;
    }

    //Recipe Generator viw
    private javafx.scene.Node buildGeminiRecipeView(Stage stage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("AI Recipe Generator");
        title.getStyleClass().add("header-label");

        // A ScrollPane with a Label to display the recipe
        Label recipeDisplay = new Label("Click the button below to see what you can cook!");
        recipeDisplay.setWrapText(true);
        recipeDisplay.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

        ScrollPane scroll = new ScrollPane(recipeDisplay);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(400);
        scroll.setStyle("-fx-background-color: transparent;");

        Button generateBtn = new Button("👨‍🍳 Generate Recipe from Pantry");
        generateBtn.getStyleClass().add("button-add");

        generateBtn.setOnAction(e -> {
            recipeDisplay.setText("Gemini is looking through your fridge... please wait...");
            generateBtn.setDisable(true);

            // Run this in a thread so the UI doesn't freeze
            new Thread(() -> {
                String recipe = fetchRecipeFromAI();
                javafx.application.Platform.runLater(() -> {
                    recipeDisplay.setText(recipe != null ? recipe : "Error: Could not reach the Chef!");
                    generateBtn.setDisable(false);
                });
            }).start();
        });

        root.getChildren().addAll(title, scroll, generateBtn);
        return root;
    }

    //helper method for recipe generator (this guy fetches the recipe)

    private String fetchRecipeFromAI() {
        try {
            // 1. Get food from your Pantry URL
            String pantryUrl = "https://studev.groept.be/api/a25ee2team206/PantryScene";
            HttpResponse<String> response = connector.makeGETRequest(HttpClient.newHttpClient(), pantryUrl);

            if (response == null || response.body() == null) return "Could not load pantry data.";

            JSONArray pantryItems = new JSONArray(response.body());
            StringBuilder foodList = new StringBuilder();

            // 2. Extract just the names
            for (int i = 0; i < pantryItems.length(); i++) {
                String name = pantryItems.getJSONObject(i).optString("name", "");
                if (!name.isEmpty()) {
                    foodList.append(name).append(", ");
                }
            }

            // 3. Build the prompt
            String fullPrompt = "Generate a simple, appetizing recipe involving some or all of these foods: "
                    + foodList.toString()
                    + ". Keep the response concise and formatted with clear steps.";

            // 4. Call your Gemini class
            CallingGeminiFromJava geminiChef = new CallingGeminiFromJava();
            return geminiChef.generateResponse(fullPrompt);

        } catch (Exception ex) {
            System.err.println("Gemini Error: " + ex.getMessage());
            return "The AI Chef ran into a problem.";
        }
    }

    //storage manager
    private void openManageStorageWindow() {
        Stage manageStage = new Stage();
        manageStage.setTitle("Manage Storage Locations");

        // Main layout container
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        // List container for existing storages
        VBox list = new VBox(10);
        list.setPadding(new Insets(10));

        // Warning label for constraints (in-use storages)
        Label warningLabel = new Label("");
        warningLabel.getStyleClass().add("label-warn");
        warningLabel.setWrapText(true);

        // --- SECTION: ADD NEW STORAGE ---
        Label addLabel = new Label("Add New Storage Location:");
        addLabel.getStyleClass().add("section-title");

        TextField newStorageField = new TextField();
        newStorageField.setPromptText("e.g., Basement Fridge");

        Button addBtn = new Button("Add");
        addBtn.setPrefWidth(80);
        addBtn.getStyleClass().add("button-add");

        addBtn.setOnAction(e -> {
            String name = newStorageField.getText().trim();
            if (!name.isEmpty()) {
                String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
                String addUrl = "https://studev.groept.be/api/a25ee2team206/addStorage/" + encodedName;
                connector.makeGETRequest(HttpClient.newHttpClient(), addUrl);
                manageStage.close();
                openManageStorageWindow(); // Refresh
            }
        });

        HBox addBox = new HBox(10, newStorageField, addBtn);
        addBox.setAlignment(Pos.CENTER_LEFT);

        // --- SECTION: LIST & EDIT/DELETE ---
        try {
            String fetchUrl = "https://studev.groept.be/api/a25ee2team206/getStorage";
            HttpResponse<String> response = connector.makeGETRequest(HttpClient.newHttpClient(), fetchUrl);

            if (response != null && response.body() != null) {
                JSONArray array = new JSONArray(response.body());
                for (int i = 0; i < array.length(); i++) {
                    String storageName = array.getJSONObject(i).getString("name");

                    Label nameLabel = new Label(storageName);
                    nameLabel.setPrefWidth(140);

                    // EDIT BUTTON
                    Button editBtn = new Button("Edit");
                    editBtn.setOnAction(e -> {
                        Stage editStage = new Stage();
                        editStage.setTitle("Rename: " + storageName);
                        TextField renameField = new TextField(storageName);
                        Button confirmBtn = new Button("Save Change");
                        confirmBtn.getStyleClass().add("button-add");

                        confirmBtn.setOnAction(ev -> {
                            String newName = renameField.getText().trim();
                            if (!newName.isEmpty() && !newName.equals(storageName)) {
                                String encOld = URLEncoder.encode(storageName, StandardCharsets.UTF_8);
                                String encNew = URLEncoder.encode(newName, StandardCharsets.UTF_8);
                                String updateUrl = "https://studev.groept.be/api/a25ee2team206/editStorage/" + encNew + "/" + encOld;

                                connector.makeGETRequest(HttpClient.newHttpClient(), updateUrl);
                                editStage.close();
                                manageStage.close();
                                openManageStorageWindow();
                            }
                        });

                        VBox editLayout = new VBox(10, new Label("Enter new name:"), renameField, confirmBtn);
                        editLayout.setPadding(new Insets(20));
                        Scene editScene = new Scene(editLayout, 300, 150);
                        applyStylesheet(editScene);
                        editStage.setScene(editScene);
                        editStage.show();
                    });

                    // DELETE BUTTON
                    Button delBtn = new Button("✕");
                    delBtn.getStyleClass().addAll("button-delete", "button-x");

                    delBtn.setOnAction(e -> {
                        String encodedName = URLEncoder.encode(storageName, StandardCharsets.UTF_8);
                        String delUrl = "https://studev.groept.be/api/a25ee2team206/deleteStorage/" + encodedName;
                        HttpResponse<String> delResponse = connector.makeGETRequest(HttpClient.newHttpClient(), delUrl);

                        // Check for DB constraint (storage in use)
                        if (delResponse == null || delResponse.statusCode() >= 400 || delResponse.body().contains("constraint")) {
                            warningLabel.setText("Cannot delete '" + storageName + "'. Items are currently stored there!");
                        } else {
                            manageStage.close();
                            openManageStorageWindow();
                        }
                    });

                    HBox row = new HBox(10, nameLabel, editBtn, delBtn);
                    row.setAlignment(Pos.CENTER_LEFT);
                    list.getChildren().add(row);
                }
            }
        } catch (Exception e) {
            list.getChildren().add(new Label("Error connecting to database."));
        }

        // ScrollPane for long lists of storage
        ScrollPane scroll = new ScrollPane(list);
        scroll.setPrefViewportHeight(300);
        scroll.setFitToWidth(true);

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> manageStage.close());

        // Assemble the UI
        root.getChildren().addAll(
                addLabel, addBox,
                new javafx.scene.control.Separator(),
                new Label("Existing Locations:"),
                scroll,
                warningLabel,
                closeBtn
        );

        Scene scene = new Scene(root, 400, 550);
        applyStylesheet(scene);
        manageStage.setScene(scene);
        manageStage.show();
    }

    //toggle list view of all storage
    private javafx.scene.Node buildStorageView(Stage stage) {
        // 1. Create a root VBox to hold the Refresh Button + the Scrollable content
        VBox root = new VBox(15);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.TOP_CENTER);

        // 2. Add the Refresh Button
        Button refreshBtn = new Button("🔄 Refresh Storage View");
        refreshBtn.getStyleClass().add("button-add"); // Using your Teal palette class

        refreshBtn.setOnAction(e -> {
            Scene scene = stage.getScene();
            if (scene != null && scene.getRoot() instanceof BorderPane) {
                BorderPane mainLayout = (BorderPane) scene.getRoot();
                mainLayout.setCenter(buildStorageView(stage));
            }
        });

        // 3. The inner container for the TitledPanes
        VBox container = new VBox(10);
        container.setPadding(new Insets(10));

        HttpClient client = HttpClient.newHttpClient();
        String url = "https://studev.groept.be/api/a25ee2team206/storageAccordion";
        HttpResponse<String> response = connector.makeGETRequest(client, url);

        if (response != null && response.body() != null) {
            JSONArray allFood = new JSONArray(response.body());

            java.util.Map<String, java.util.List<JSONObject>> groupedData = new java.util.HashMap<>();
            for (int i = 0; i < allFood.length(); i++) {
                JSONObject food = allFood.getJSONObject(i);
                String storageName = food.optString("storage", "Other");
                groupedData.computeIfAbsent(storageName, k -> new java.util.ArrayList<>()).add(food);
            }

            for (String storageName : groupedData.keySet()) {
                VBox sectionLayout = new VBox(10);
                sectionLayout.setPadding(new Insets(10));

                Button addInStorageBtn = new Button("+ Add item to " + storageName);
                addInStorageBtn.getStyleClass().add("button-add");
                addInStorageBtn.setOnAction(e -> showAddFoodWindow(stage, container, null, storageName));

                GridPane grid = new GridPane();
                grid.setHgap(15);
                grid.setVgap(10);
                grid.setPadding(new Insets(10));

                grid.add(headerLabel("Name"), 0, 0);
                grid.add(headerLabel("Amount"), 1, 0);
                grid.add(headerLabel("Days Left"), 2, 0);
                grid.add(headerLabel("Actions"), 3, 0, 2, 1);

                java.util.List<JSONObject> items = groupedData.get(storageName);
                for (int j = 0; j < items.size(); j++) {
                    JSONObject food = items.get(j);
                    int row = j + 1;

                    String name = food.optString("name", "Unknown");
                    String amount = food.optString("amount", "0");
                    String unit = food.isNull("unit") ? "" : food.getString("unit");
                    int days = food.optInt("days_left", 0);

                    Label daysLabel = new Label(String.valueOf(days));
                    applyUrgencyStyle(daysLabel, days);

                    Button editBtn = new Button("Edit");
                    editBtn.setOnAction(e -> showEditFoodWindow(stage, grid, name));

                    // UPDATE: Delete Button Logic
                    Button delBtn = new Button("Delete");
                    delBtn.getStyleClass().add("button-delete");
                    delBtn.setOnAction(e -> {
                        // We pass the Runnable to refresh specifically the Storage View
                        deleteFood(name, stage, () -> {
                            Scene scene = stage.getScene();
                            if (scene != null && scene.getRoot() instanceof BorderPane) {
                                BorderPane mainLayout = (BorderPane) scene.getRoot();
                                mainLayout.setCenter(buildStorageView(stage));
                            }
                        });
                    });

                    grid.add(new Label(name), 0, row);
                    grid.add(new Label(amount + " " + unit), 1, row);
                    grid.add(daysLabel, 2, row);
                    grid.add(editBtn, 3, row);
                    grid.add(delBtn, 4, row);
                }

                sectionLayout.getChildren().addAll(addInStorageBtn, grid);
                TitledPane section = new TitledPane(storageName.toUpperCase(), sectionLayout);
                section.setExpanded(false);
                container.getChildren().add(section);
            }
        } else {
            container.getChildren().add(new Label("No storage data found."));
        }

        // 4. Wrap everything up
        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);

        root.getChildren().addAll(refreshBtn, scroll);

        return root;
    }

    //toggle list by category
    private javafx.scene.Node buildCategoryView(Stage stage) {
        // 1. Create a parent container to hold the button and the scrollable list
        VBox root = new VBox(15);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.TOP_CENTER);

        // 2. Create the Refresh Button
        Button refreshBtn = new Button("🔄 Refresh Categories");
        refreshBtn.getStyleClass().add("button-add"); // Uses your Teal color

        // 3. The existing container for the TitledPanes
        VBox container = new VBox(10);
        container.setPadding(new Insets(20));

        // 4. Setup Refresh Logic
        // We update the center of the main layout by re-calling the build method
        refreshBtn.setOnAction(e -> {
            // Find the main BorderPane to swap the center content
            Scene scene = stage.getScene();
            if (scene != null && scene.getRoot() instanceof BorderPane) {
                BorderPane mainLayout = (BorderPane) scene.getRoot();
                mainLayout.setCenter(buildCategoryView(stage));
            }
        });

        // --- YOUR EXISTING DATA FETCHING LOGIC ---
        String url = "https://studev.groept.be/api/a25ee2team206/categoryAccordion";
        HttpResponse<String> response = connector.makeGETRequest(HttpClient.newHttpClient(), url);

        if (response != null && response.body() != null) {
            JSONArray allFood = new JSONArray(response.body());

            // Group the food items by category
            java.util.Map<String, java.util.List<JSONObject>> groupedData = new java.util.HashMap<>();
            for (int i = 0; i < allFood.length(); i++) {
                JSONObject food = allFood.getJSONObject(i);
                String catName = food.isNull("category") || food.optString("category").isBlank()
                        ? "Uncategorized"
                        : food.getString("category");
                groupedData.computeIfAbsent(catName, k -> new java.util.ArrayList<>()).add(food);
            }

            // Create a TitledPane for each category group
            for (String catName : groupedData.keySet()) {
                VBox sectionLayout = new VBox(10);
                sectionLayout.setPadding(new Insets(10));

                Button addInCategoryBtn = new Button("+ Add item to " + catName);
                addInCategoryBtn.getStyleClass().add("button-add");
                addInCategoryBtn.setOnAction(e -> showAddFoodWindow(stage, container, catName, null));

                GridPane grid = new GridPane();
                grid.setHgap(15);
                grid.setVgap(10);

                grid.add(new Label("Name"), 0, 0);
                grid.add(new Label("Amount"), 1, 0);
                grid.add(new Label("Days Left"), 2, 0);
                grid.add(new Label("Actions"), 3, 0, 2, 1);

                java.util.List<JSONObject> items = groupedData.get(catName);
                for (int j = 0; j < items.size(); j++) {
                    JSONObject food = items.get(j);
                    int row = j + 1;
                    String name = food.optString("name", "Unknown");
                    int days = food.optInt("days_left", 0);

                    Label daysLabel = new Label(String.valueOf(days));
                    // applyUrgencyStyle(daysLabel, days); // Use your existing helper if available

                    Button editBtn = new Button("Edit");
                    editBtn.setOnAction(e -> showEditFoodWindow(stage, grid, name));

                    Button delBtn = new Button("Delete");
                    delBtn.getStyleClass().add("button-delete");
                    // Inside the loop in buildCategoryView
                    delBtn.setOnAction(e -> {
                        deleteFood(name, stage, () -> {
                            // This is the SAME logic you used for the refresh button
                            Scene scene = stage.getScene();
                            if (scene != null && scene.getRoot() instanceof BorderPane) {
                                BorderPane mainLayout = (BorderPane) scene.getRoot();
                                mainLayout.setCenter(buildCategoryView(stage));
                            }
                        });
                    });

                    grid.add(new Label(name), 0, row);
                    grid.add(new Label(food.optString("amount") + " " + (food.isNull("unit") ? "" : food.getString("unit"))), 1, row);
                    grid.add(daysLabel, 2, row);
                    grid.add(editBtn, 3, row);
                    grid.add(delBtn, 4, row);
                }

                sectionLayout.getChildren().addAll(addInCategoryBtn, grid);
                TitledPane section = new TitledPane(catName.toUpperCase(), sectionLayout);
                section.setExpanded(false);
                container.getChildren().add(section);
            }
        } else {
            container.getChildren().add(new Label("No food data found for categories."));
        }

        // 5. Wrap the container in a ScrollPane
        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);

        // 6. Final assembly: Add the refresh button and the scrollable list to the root VBox
        root.getChildren().addAll(refreshBtn, scroll);

        return root;
    }

    //urgency view
    private javafx.scene.Node buildFreshnessView(Stage stage) {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.TOP_CENTER);

        // Headers
        grid.add(headerLabel("Urgency"), 0, 0);
        grid.add(headerLabel("Name"), 1, 0);
        grid.add(headerLabel("Category"), 2, 0);
        grid.add(headerLabel("Amount"), 3, 0);
        grid.add(headerLabel("Location"), 4, 0);

        // Fetch the pre-sorted data
        String url = "https://studev.groept.be/api/a25ee2team206/getSortedByUrgency"; // Update this to your actual URL
        HttpResponse<String> response = connector.makeGETRequest(HttpClient.newHttpClient(), url);

        if (response != null && response.body() != null) {
            JSONArray array = new JSONArray(response.body());

            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                int row = i + 1;

                // Extract data (ensure these keys match your JSON response exactly)
                int daysInt = o.optInt("days_left", 0);
                String urgency = String.valueOf(daysInt);
                String name = o.optString("name", "Unknown");
                String category = o.optString("category", "-");
                String amount = o.optString("amount", "0") + " " + o.optString("unit", "");
                String location = o.optString("storage", "Unknown");

                Label urgencyLabel = new Label(urgency);
                // Highlight urgent items
                applyUrgencyStyle(urgencyLabel, daysInt);

                grid.add(urgencyLabel, 0, row);
                grid.add(new Label(name), 1, row);
                grid.add(new Label(category), 2, row);
                grid.add(new Label(amount), 3, row);
                grid.add(new Label(location), 4, row);
            }
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        return scroll;
    }

    //shopping list refresher
    private void refreshShoppingList(VBox listContainer) {
        listContainer.getChildren().clear();
        HttpClient client = HttpClient.newHttpClient();
        // Using your standard URL pattern
        String url = "https://studev.groept.be/api/a25ee2team206/getShoppingList";

        try {
            HttpResponse<String> response = connector.makeGETRequest(client, url);
            if (response != null && response.body() != null) {
                JSONArray array = new JSONArray(response.body());

                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    String itemName = o.optString("name", "Unknown");

                    Button removeBtn = new Button("Remove");
                    removeBtn.getStyleClass().add("button-delete");

                    removeBtn.setOnAction(e -> {
                        // Standard URL encoding pattern
                        String encName = URLEncoder.encode(itemName, StandardCharsets.UTF_8);
                        connector.makeGETRequest(HttpClient.newHttpClient(),
                                "https://studev.groept.be/api/a25ee2team206/removeFromShoppingList/" + encName);

                        // Refresh the specific container
                        refreshShoppingList(listContainer);
                    });

                    listContainer.getChildren().add(new HBox(10, new Label(itemName), removeBtn));
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load shopping list: " + e.getMessage());
            listContainer.getChildren().add(new Label("Failed to load list."));
        }
    }

    //shopping list
    private javafx.scene.Node buildShoppingListView(Stage stage) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label title = new Label("Shopping List:");
        title.getStyleClass().add("section-title");

        // Input area
        TextField nameInput = new TextField();
        nameInput.setPromptText("New item name...");

        Button addBtn = new Button("Add to List");
        addBtn.getStyleClass().add("button-add");

        // Container for the dynamic list
        VBox listContainer = new VBox(10);

        addBtn.setOnAction(e -> {
            if (!nameInput.getText().isBlank()) {
                String encName = URLEncoder.encode(nameInput.getText(), StandardCharsets.UTF_8);
                connector.makeGETRequest(HttpClient.newHttpClient(),
                        "https://studev.groept.be/api/a25ee2team206/addToShoppingList/" + encName);
                nameInput.clear();

                // Refresh
                refreshShoppingList(listContainer);
            }
        });

        // Populate the container
        refreshShoppingList(listContainer);

        root.getChildren().addAll(
                title,
                new HBox(10, nameInput, addBtn),
                new Separator(),
                listContainer
        );

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        return scroll;
    }


    @Override
    public void start(Stage primaryStage) {
        connector = new Connector();

        //The Main Frame
        BorderPane mainLayout = new BorderPane();

        //The Header with the drop down of all th eoptnos
        ComboBox<String> viewSwitcher = new ComboBox<>();
        viewSwitcher.getItems().addAll("All Food", "By Storage Location", "By Category", "By Urgency");
        viewSwitcher.setValue("All Food"); // Set default

        //storage manager stuffs
        Button manageStorageBtn = new Button("Manage Storage");
        manageStorageBtn.setOnAction(e -> openManageStorageWindow());

        //addfoodbtn
        Button globalAddBtn = new Button("+ Add New Food");
        globalAddBtn.getStyleClass().add("button-add");
        globalAddBtn.setOnAction(e -> showAddFoodWindow(primaryStage, null, null, null));

        //shopping list button
        Button shoppingListBtn = new Button("Shopping List");
        shoppingListBtn.setOnAction(e -> {
            // 1. Manually clear the viewSwitcher selection so it doesn't
            // conflict with the current display
            viewSwitcher.getSelectionModel().clearSelection();

            // 2. Set the center directly
            mainLayout.setCenter(buildShoppingListView(primaryStage));
        });

        //fridge sensor info display button
        Button fridgeInfoBtn = new Button("Fridge Info");
        fridgeInfoBtn.setOnAction(e -> {
            // 1. Manually clear the viewSwitcher selection so it doesn't
            // conflict with the current display
            viewSwitcher.getSelectionModel().clearSelection();

            // 2. Set the center directly
            mainLayout.setCenter(buildFridgeInfoView(primaryStage));
        });

        //Diettime access times menu button
        Button accessTimeBtn = new Button("Access Time");
        accessTimeBtn.setOnAction(e -> {
            // 1. Manually clear the viewSwitcher selection so it doesn't
            // conflict with the current display
            viewSwitcher.getSelectionModel().clearSelection();

            // 2. Set the center directly
            mainLayout.setCenter(buildAccessTimeView(primaryStage));
        });

        //Lazysusan menu button
        Button lazySusanBtn = new Button("Lazy Susan");
        lazySusanBtn.setOnAction(e -> {
            // 1. Manually clear the viewSwitcher selection so it doesn't
            // conflict with the current display
            viewSwitcher.getSelectionModel().clearSelection();

            // 2. Set the center directly
            mainLayout.setCenter(buildLazySusanView(primaryStage, mainLayout));
        });

        //Recipe generator with AI API call
        Button recipeGeneratorBtn = new Button("Generate Recipe");
        recipeGeneratorBtn.setOnAction(e -> {
            // 1. Manually clear the viewSwitcher selection so it doesn't
            // conflict with the current display
            viewSwitcher.getSelectionModel().clearSelection();

            // 2. Set the center directly
            mainLayout.setCenter(buildGeminiRecipeView(primaryStage));
        });

        // Reorganizing the header into a GridPane for a neat 2-row (3-column) layout
        GridPane headerGrid = new GridPane();
        headerGrid.setPadding(new Insets(20));
        headerGrid.setHgap(15);
        headerGrid.setVgap(10);
        headerGrid.setAlignment(Pos.CENTER_LEFT);
        headerGrid.getStyleClass().add("header");

        // Add a nice label and put them in a group for the first cell
        Label viewLabel = new Label("Select View:");
        viewLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        HBox switcherGroup = new HBox(10, viewLabel, viewSwitcher);
        switcherGroup.setAlignment(Pos.CENTER_LEFT);

        // Row 1: View Switcher, Storage Management, Global Add, shopping list
        headerGrid.add(switcherGroup, 0, 0);
        headerGrid.add(manageStorageBtn, 1, 0);
        headerGrid.add(globalAddBtn, 2, 0);
        headerGrid.add(shoppingListBtn, 3, 0);
        // Row 2: Lazy susan, Fridge Info, Access Times, recipe generator
        headerGrid.add(recipeGeneratorBtn, 0, 1);
        headerGrid.add(lazySusanBtn, 1, 1);
        headerGrid.add(fridgeInfoBtn, 2, 1);
        headerGrid.add(accessTimeBtn, 3, 1);
        // (Your 6th button will go in column 3, row 1 later)

        // Ensure buttons have a consistent width for symmetry
        for (Node node : headerGrid.getChildren()) {
            if (node instanceof Button) {
                ((Button) node).setPrefWidth(150);
                ((Button) node).setMaxWidth(Double.MAX_VALUE);
            }
        }

        mainLayout.setTop(headerGrid);

        // 3. Listener to trigger the "Swap"
        viewSwitcher.setOnAction(e -> {
            String selectedView = viewSwitcher.getValue();
            // Adding a null check because clearSelection() triggers this listener
            if (selectedView != null) {
                updateMainContent(selectedView, mainLayout, primaryStage);
            }
        });

        // 4. Load the initial view
        updateMainContent("All Food", mainLayout, primaryStage);

        // Set the Scene
        Scene scene = new Scene(mainLayout, 850, 650);
        // Note the double slash: /ee2/smortfridge/StylishFridge.css
        java.net.URL cssUrl = getClass().getResource("/ee2/smortfridge/StylishFridge.css");

        //adding a cool icon :3
        if (getClass().getResource("/ee2/smortfridge/EE2Logosmall.png") != null) {
            Image icon = new Image(getClass().getResourceAsStream("/ee2/smortfridge/EE2Logosmall.png"));
            primaryStage.getIcons().add(icon);
        }

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Could not find CSS at: /ee2/smortfridge/StylishFridge.css");
        }
        primaryStage.setTitle("Smart Fridge Manager");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // The "Brain" that swaps the views
    private void updateMainContent(String viewName, BorderPane mainLayout, Stage stage) {
        if ("All Food".equals(viewName)) {
            mainLayout.setCenter(buildAllFoodGrid(stage));
        } else if ("By Storage Location".equals(viewName)) {
            mainLayout.setCenter(buildStorageView(stage));
        } else if ("By Category".equals(viewName)) {
            mainLayout.setCenter(buildCategoryView(stage));
        } else if("By Urgency".equals(viewName)) {
            mainLayout.setCenter(buildFreshnessView(stage));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
