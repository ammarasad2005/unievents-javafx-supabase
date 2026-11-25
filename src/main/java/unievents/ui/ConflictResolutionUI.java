package unievents.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import unievents.application.ManagementController;
import unievents.shared.ConflictDetails;
import unievents.shared.ConflictStatus;
import unievents.shared.ConflictSummary;
import unievents.shared.RegistrationSummary;
import unievents.shared.ResolutionType;

import java.util.List;

/**
 * UC12 (Resolve Registration Conflict — Admin inspects open conflicts, registration history & waitlist, and resolves).
 * Maps directly to :ConflictResolutionUI in SD-12.
 */
public class ConflictResolutionUI extends VBox {
    private final ManagementController controller;
    private final TableView<ConflictSummary> conflictsTable = new TableView<>();
    private final Label detailHeader = new Label("Select an open conflict to inspect");
    private final Label detailMeta = new Label("");
    private final ListView<String> concernedList = new ListView<>();
    private final ListView<String> waitlistView = new ListView<>();
    private final ComboBox<ResolutionType> resolutionCombo = new ComboBox<>();
    private final TextArea reasonArea = new TextArea();
    private final Button resolveBtn = new Button("Apply Resolution & Mark RESOLVED (UC12)");
    private final Label banner = UiComponents.createBanner();

    private Integer selectedConflictId;

    public ConflictResolutionUI(ManagementController controller) {
        this.controller = controller;
        setSpacing(14);
        setPadding(new Insets(20, 24, 24, 24));
        buildView();
        refreshData();
    }

    private void buildView() {
        Label title = new Label("Registration Conflict Resolution");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("UC12 · Review open overbooking/capacity conflicts, inspect waitlist & seat history, and record an auditable resolution.");
        subtitle.getStyleClass().add("page-subtitle");

        TableColumn<ConflictSummary, String> colId = new TableColumn<>("Conflict #");
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().conflictId()));
        colId.setPrefWidth(80);

        TableColumn<ConflictSummary, String> colEvent = new TableColumn<>("Event");
        colEvent.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().eventTitle()));
        colEvent.setPrefWidth(190);

        TableColumn<ConflictSummary, String> colStudent = new TableColumn<>("Concerned Student(s)");
        colStudent.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fullName()));
        colStudent.setPrefWidth(165);

        TableColumn<ConflictSummary, String> colType = new TableColumn<>("Conflict Type");
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().type().name()));
        colType.setPrefWidth(140);

        TableColumn<ConflictSummary, String> colDetected = new TableColumn<>("Detected At");
        colDetected.setCellValueFactory(c -> new SimpleStringProperty(UiComponents.formatDateTime(c.getValue().detectedAt())));
        colDetected.setPrefWidth(150);

        conflictsTable.getColumns().setAll(List.of(colId, colEvent, colStudent, colType, colDetected));
        conflictsTable.setPlaceholder(new Label("✓ Zero open registration conflicts — all campus event records are consistent!"));
        VBox.setVgrow(conflictsTable, Priority.ALWAYS);

        conflictsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) {
                inspectConflict(sel.conflictId());
            } else {
                clearInspector();
            }
        });

        Button refreshBtn = new Button("Refresh Open Conflicts");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> {
            UiComponents.hideBanner(banner);
            refreshData();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox leftPane = new VBox(10,
                new HBox(10, UiComponents.fieldLabel("Open Registration Conflicts"), spacer, refreshBtn) {{
                    setAlignment(Pos.CENTER_LEFT);
                }},
                conflictsTable
        );
        leftPane.getStyleClass().add("card");

        // Right inspector
        detailHeader.getStyleClass().add("section-heading");
        detailMeta.setWrapText(true);
        concernedList.setPrefHeight(90);
        waitlistView.setPrefHeight(90);

        resolutionCombo.setItems(FXCollections.observableArrayList(ResolutionType.values()));
        resolutionCombo.setValue(ResolutionType.MOVE_TO_WAITLIST);
        resolutionCombo.setMaxWidth(Double.MAX_VALUE);

        reasonArea.setPromptText("Mandatory audit justification (e.g., Moved excess bulk-imported registration to waitlist to preserve 2-seat lab limit)...");
        reasonArea.setPrefRowCount(3);

        resolveBtn.getStyleClass().add("btn-primary");
        resolveBtn.setDisable(true);
        resolveBtn.setOnAction(e -> handleResolve());

        VBox rightCard = new VBox(9,
                detailHeader,
                detailMeta,
                new Separator(),
                UiComponents.fieldLabel("Concerned Registration(s) & Event History"),
                concernedList,
                UiComponents.fieldLabel("Current Event Waitlist"),
                waitlistView,
                new Separator(),
                UiComponents.fieldLabel("Resolution Policy *"),
                resolutionCombo,
                UiComponents.fieldLabel("Audit Reason *"),
                reasonArea,
                resolveBtn
        );
        rightCard.getStyleClass().add("card");
        rightCard.setMinWidth(410);

        SplitPane split = new SplitPane(leftPane, rightCard);
        split.setDividerPositions(0.53);
        VBox.setVgrow(split, Priority.ALWAYS);

        getChildren().addAll(new VBox(2, title, subtitle), banner, split);
    }

    public void refreshData() {
        try {
            List<ConflictSummary> items = controller.getOpenConflicts();
            conflictsTable.setItems(FXCollections.observableArrayList(items));
            if (!items.isEmpty()) {
                conflictsTable.getSelectionModel().selectFirst();
            } else {
                clearInspector();
            }
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void inspectConflict(int conflictId) {
        try {
            ConflictDetails d = controller.getConflictDetails(conflictId);
            selectedConflictId = d.conflictId();
            detailHeader.setText("Conflict #" + d.conflictId() + " · " + d.eventTitle());
            detailMeta.setText("Type: " + d.type()
                    + "   |   Event Capacity: " + d.capacity()
                    + "   |   Active Confirmed Seats: " + d.registeredCount()
                    + (d.reason() != null && !d.reason().isBlank() ? "\nSystem Note: " + d.reason() : ""));

            List<String> regLines = d.registrationHistory().stream()
                    .map((RegistrationSummary r) -> {
                        boolean flagged = d.concernedRegistrations().stream()
                                .anyMatch(cr -> cr.registrationId() == r.registrationId());
                        return (flagged ? "⚠ [CONFLICTED] " : "   ")
                                + "Reg #" + r.registrationId() + " · " + r.studentName()
                                + " (" + r.rollNumber() + ") — " + r.status()
                                + (r.waitlistPosition() != null ? " #" + r.waitlistPosition() : "");
                    })
                    .toList();
            concernedList.setItems(FXCollections.observableArrayList(regLines));

            List<String> wlLines = d.waitlist().stream()
                    .map(r -> "#" + r.waitlistPosition() + " · " + r.studentName() + " (" + r.rollNumber() + ")")
                    .toList();
            waitlistView.setItems(FXCollections.observableArrayList(
                    wlLines.isEmpty() ? List.of("Waitlist is currently empty.") : wlLines));

            resolveBtn.setDisable(false);
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void handleResolve() {
        if (selectedConflictId == null) {
            return;
        }
        try {
            ConflictStatus updated = controller.resolveConflict(
                    selectedConflictId,
                    resolutionCombo.getValue(),
                    reasonArea.getText()
            );
            UiComponents.showSuccess(banner, "Conflict #" + selectedConflictId + " marked " + updated
                    + " using policy " + resolutionCombo.getValue() + ".");
            reasonArea.clear();
            refreshData();
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void clearInspector() {
        selectedConflictId = null;
        detailHeader.setText("No open conflict selected");
        detailMeta.setText("");
        concernedList.getItems().clear();
        waitlistView.getItems().clear();
        resolveBtn.setDisable(true);
    }
}
