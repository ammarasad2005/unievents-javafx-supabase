package unievents.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import unievents.application.ManagementController;
import unievents.domain.Club;
import unievents.domain.ClubOrganizer;

import java.util.ArrayList;
import java.util.List;

/**
 * UC11 (Create Club — Admin creates a campus society and optionally assigns a President organizer).
 * Maps directly to :CreateClubUI in SD-11.
 */
public class CreateClubUI extends VBox {
    private final ManagementController controller;
    private final TextField nameField = new TextField();
    private final TextArea descArea = new TextArea();
    private final ComboBox<PresidentOption> presidentCombo = new ComboBox<>();
    private final TableView<Club> clubsTable = new TableView<>();
    private final Label banner = UiComponents.createBanner();

    private record PresidentOption(Integer organizerId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public CreateClubUI(ManagementController controller) {
        this.controller = controller;
        setSpacing(14);
        setPadding(new Insets(20, 24, 24, 24));
        buildView();
        refreshData();
    }

    private void buildView() {
        Label title = new Label("Campus Clubs & Create Official Society");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("UC11 · Register official student societies with unique names and optionally assign a Club President.");
        subtitle.getStyleClass().add("page-subtitle");

        nameField.setPromptText("e.g. Robotics & Embedded Systems Society");
        descArea.setPromptText("Mission statement, activities, and focus areas...");
        descArea.setPrefRowCount(4);
        presidentCombo.setMaxWidth(Double.MAX_VALUE);

        Button createBtn = new Button("Create Official Club (UC11)");
        createBtn.getStyleClass().add("btn-success");
        createBtn.setOnAction(e -> handleCreate());

        Button resetBtn = new Button("Clear");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setOnAction(e -> {
            nameField.clear();
            descArea.clear();
            if (!presidentCombo.getItems().isEmpty()) {
                presidentCombo.getSelectionModel().selectFirst();
            }
        });

        Label formHeader = new Label("Register New Club");
        formHeader.getStyleClass().add("section-heading");

        VBox formCard = new VBox(10,
                formHeader,
                UiComponents.fieldLabel("Club / Society Name * (must be unique)"),
                nameField,
                UiComponents.fieldLabel("Description"),
                descArea,
                UiComponents.fieldLabel("Assign Club President (Optional)"),
                presidentCombo,
                new HBox(10, createBtn, resetBtn) {{ setAlignment(Pos.CENTER_LEFT); }}
        );
        formCard.getStyleClass().add("card");
        formCard.setMinWidth(380);

        TableColumn<Club, String> colId = new TableColumn<>("Club #");
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getClubId()));
        colId.setPrefWidth(65);

        TableColumn<Club, String> colName = new TableColumn<>("Society Name");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colName.setPrefWidth(200);

        TableColumn<Club, String> colActive = new TableColumn<>("State");
        colActive.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "ACTIVE" : "INACTIVE"));
        colActive.setPrefWidth(85);

        TableColumn<Club, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colDesc.setPrefWidth(310);

        clubsTable.getColumns().setAll(List.of(colId, colName, colActive, colDesc));
        VBox.setVgrow(clubsTable, Priority.ALWAYS);

        Label dirHeader = new Label("Active Campus Societies Directory");
        dirHeader.getStyleClass().add("section-heading");
        VBox tableCard = new VBox(10, dirHeader, clubsTable);
        tableCard.getStyleClass().add("card");

        SplitPane split = new SplitPane(formCard, tableCard);
        split.setDividerPositions(0.42);
        VBox.setVgrow(split, Priority.ALWAYS);

        getChildren().addAll(new VBox(2, title, subtitle), banner, split);
    }

    public void refreshData() {
        try {
            List<PresidentOption> opts = new ArrayList<>();
            opts.add(new PresidentOption(null, "(None — Assign President Later)"));
            for (ClubOrganizer org : controller.getAvailableOrganizers()) {
                opts.add(new PresidentOption(
                        org.getUserId(),
                        org.getFullName() + " (" + org.getEmail() + (org.isPresident() ? " · Current President" : " · Available") + ")"
                ));
            }
            presidentCombo.setItems(FXCollections.observableArrayList(opts));
            presidentCombo.getSelectionModel().selectFirst();
            clubsTable.setItems(FXCollections.observableArrayList(controller.getAllClubs()));
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void handleCreate() {
        try {
            PresidentOption opt = presidentCombo.getValue();
            Integer presidentId = opt != null ? opt.organizerId() : null;
            Club created = controller.createClub(nameField.getText(), descArea.getText(), presidentId);
            UiComponents.showSuccess(banner, "Created Club #" + created.getClubId() + " \"" + created.getName() + "\""
                    + (presidentId != null ? " and assigned selected organizer as President." : "."));
            nameField.clear();
            descArea.clear();
            refreshData();
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }
}
