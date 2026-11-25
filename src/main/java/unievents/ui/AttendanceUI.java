package unievents.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import unievents.application.ManagementController;
import unievents.shared.AttendanceRow;
import unievents.shared.AttendanceSheet;
import unievents.shared.EventSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * UC9 (Mark Attendance — displays event check-in code & registrant checklist, marks MANUAL attendance).
 * Maps directly to :AttendanceUI in SD-09.
 */
public class AttendanceUI extends VBox {
    private final ManagementController controller;
    private final ComboBox<EventSummary> eventCombo = new ComboBox<>();
    private final Label codeBadge = UiComponents.statusBadge("Check-In Code: —");
    private final TableView<SelectableRow> table = new TableView<>();
    private final Label banner = UiComponents.createBanner();

    private static final class SelectableRow {
        private final AttendanceRow row;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);

        SelectableRow(AttendanceRow row) {
            this.row = row;
        }

        public AttendanceRow row() { return row; }
        public BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean val) {
            if (!row.alreadyCheckedIn()) {
                selected.set(val);
            }
        }
    }

    public AttendanceUI(ManagementController controller) {
        this.controller = controller;
        setSpacing(14);
        setPadding(new Insets(20, 24, 24, 24));
        buildView();
        refreshData(null);
    }

    private void buildView() {
        Label title = new Label("Attendance Sheet & Manual Check-In");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("UC9 · View confirmed registrants, share the self check-in code, or record manual attendance.");
        subtitle.getStyleClass().add("page-subtitle");

        eventCombo.setPrefWidth(420);
        eventCombo.valueProperty().addListener((obs, o, sel) -> loadSheet(sel));

        Button selectPendingBtn = new Button("Select All Pending");
        selectPendingBtn.getStyleClass().add("btn-secondary");
        selectPendingBtn.setOnAction(e -> {
            for (SelectableRow item : table.getItems()) {
                item.setSelected(true);
            }
            table.refresh();
        });

        Button refreshBtn = new Button("Refresh Sheet");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> {
            UiComponents.hideBanner(banner);
            loadSheet(eventCombo.getValue());
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topCard = new HBox(12, UiComponents.fieldLabel("Event:"), eventCombo, codeBadge, spacer, selectPendingBtn, refreshBtn);
        topCard.setAlignment(Pos.CENTER_LEFT);
        topCard.getStyleClass().add("card");

        table.setEditable(true);

        TableColumn<SelectableRow, Boolean> colSelect = new TableColumn<>("Mark");
        colSelect.setCellValueFactory(c -> c.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);
        colSelect.setPrefWidth(65);

        TableColumn<SelectableRow, String> colRoll = new TableColumn<>("Roll Number");
        colRoll.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().row().rollNumber()));
        colRoll.setPrefWidth(120);

        TableColumn<SelectableRow, String> colName = new TableColumn<>("Student Name");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().row().fullName()));
        colName.setPrefWidth(210);

        TableColumn<SelectableRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().row().status().name()));
        colStatus.setPrefWidth(130);

        TableColumn<SelectableRow, String> colMethod = new TableColumn<>("Check-In Method");
        colMethod.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().row().method() != null ? c.getValue().row().method().name() : "—"
        ));
        colMethod.setPrefWidth(135);

        TableColumn<SelectableRow, String> colTime = new TableColumn<>("Checked-In Timestamp");
        colTime.setCellValueFactory(c -> new SimpleStringProperty(UiComponents.formatDateTime(c.getValue().row().checkedInAt())));
        colTime.setPrefWidth(180);

        table.getColumns().setAll(List.of(colSelect, colRoll, colName, colStatus, colMethod, colTime));
        table.setPlaceholder(new Label("No confirmed registrations found for this event."));
        VBox.setVgrow(table, Priority.ALWAYS);

        Button recordBtn = new Button("Record Manual Attendance for Selected Students (UC9)");
        recordBtn.getStyleClass().add("btn-success");
        recordBtn.setOnAction(e -> handleRecordAttendance());

        HBox bottomBar = new HBox(12, recordBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.getStyleClass().add("card");

        getChildren().addAll(new VBox(2, title, subtitle), banner, topCard, table, bottomBar);
    }

    public void refreshData(Integer preselectEventId) {
        try {
            List<EventSummary> events = controller.getMyEvents();
            eventCombo.setItems(FXCollections.observableArrayList(events));
            if (preselectEventId != null) {
                for (EventSummary e : events) {
                    if (e.eventId() == preselectEventId) {
                        eventCombo.setValue(e);
                        return;
                    }
                }
            }
            if (!events.isEmpty()) {
                eventCombo.getSelectionModel().selectFirst();
            }
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void loadSheet(EventSummary sel) {
        if (sel == null) {
            table.getItems().clear();
            codeBadge.setText("Check-In Code: —");
            return;
        }
        try {
            AttendanceSheet sheet = controller.getAttendanceSheet(sel.eventId());
            codeBadge.setText("Venue Code: " + sheet.checkInCode());
            List<SelectableRow> rows = sheet.students().stream()
                    .map(SelectableRow::new)
                    .toList();
            table.setItems(FXCollections.observableArrayList(rows));
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void handleRecordAttendance() {
        EventSummary sel = eventCombo.getValue();
        if (sel == null) {
            return;
        }
        List<Integer> selectedIds = new ArrayList<>();
        for (SelectableRow item : table.getItems()) {
            if (item.isSelected() && !item.row().alreadyCheckedIn()) {
                selectedIds.add(item.row().registrationId());
            }
        }
        try {
            int savedCount = controller.recordAttendance(sel.eventId(), selectedIds);
            UiComponents.showSuccess(banner, "Recorded MANUAL attendance for " + savedCount
                    + " student(s) on \"" + sel.title() + "\".");
            loadSheet(sel);
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }
}
