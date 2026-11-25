package unievents.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import unievents.application.ManagementController;
import unievents.domain.AttendanceReport;
import unievents.shared.EventSummary;
import unievents.shared.Participant;

import java.util.List;

/**
 * UC10 (Generate Attendance Report — persists AttendanceReport and displays KPIs + Participant list).
 * Maps directly to :AttendanceReportUI in SD-10.
 */
public class AttendanceReportUI extends VBox {
    private final ManagementController controller;
    private final ComboBox<EventSummary> eventCombo = new ComboBox<>();
    private final Label reportIdLabel = new Label("—");
    private final Label regCountLabel = new Label("0");
    private final Label attCountLabel = new Label("0");
    private final Label rateLabel = new Label("0%");
    private final Label generatedAtLabel = new Label("Select an event and click Generate Report");
    private final TableView<Participant> participantTable = new TableView<>();
    private final Label banner = UiComponents.createBanner();

    public AttendanceReportUI(ManagementController controller) {
        this.controller = controller;
        setSpacing(14);
        setPadding(new Insets(20, 24, 24, 24));
        buildView();
        refreshData(null);
    }

    private void buildView() {
        Label title = new Label("Attendance Reports");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Generate official attendance summaries and review participant turnout records.");
        subtitle.getStyleClass().add("page-subtitle");

        eventCombo.setPrefWidth(430);
        Button generateBtn = new Button("Generate Report");
        generateBtn.getStyleClass().add("btn-primary");
        generateBtn.setOnAction(e -> handleGenerate());

        HBox selectorBar = new HBox(12, UiComponents.fieldLabel("Club Event:"), eventCombo, generateBtn);
        selectorBar.setAlignment(Pos.CENTER_LEFT);
        selectorBar.getStyleClass().add("card");

        HBox kpiBar = new HBox(14,
                UiComponents.metricCard("Saved Report #", reportIdLabel),
                UiComponents.metricCard("Confirmed Registered", regCountLabel),
                UiComponents.metricCard("Total Attended", attCountLabel),
                UiComponents.metricCard("Turnout Rate", rateLabel)
        );
        generatedAtLabel.setStyle("-fx-text-fill: #475569;");

        TableColumn<Participant, String> colRoll = new TableColumn<>("Roll Number");
        colRoll.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().rollNumber()));
        colRoll.setPrefWidth(135);

        TableColumn<Participant, String> colName = new TableColumn<>("Participant Name");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fullName()));
        colName.setPrefWidth(230);

        TableColumn<Participant, String> colStatus = new TableColumn<>("Registration Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status().name()));
        colStatus.setPrefWidth(160);

        TableColumn<Participant, String> colMethod = new TableColumn<>("Check-In Method");
        colMethod.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().method() != null ? c.getValue().method().name() : "—"
        ));
        colMethod.setPrefWidth(140);

        TableColumn<Participant, String> colTime = new TableColumn<>("Checked-In At");
        colTime.setCellValueFactory(c -> new SimpleStringProperty(UiComponents.formatDateTime(c.getValue().checkedInAt())));
        colTime.setPrefWidth(180);

        participantTable.getColumns().setAll(List.of(colRoll, colName, colStatus, colMethod, colTime));
        participantTable.setPlaceholder(new Label("Click 'Generate & Save Attendance Report' to load participant breakdown."));
        VBox.setVgrow(participantTable, Priority.ALWAYS);

        getChildren().addAll(new VBox(2, title, subtitle), banner, selectorBar, kpiBar, generatedAtLabel, participantTable);
    }

    public void refreshData(Integer preselectEventId) {
        try {
            List<EventSummary> events = controller.getMyEvents();
            eventCombo.setItems(FXCollections.observableArrayList(events));
            if (preselectEventId != null) {
                for (EventSummary e : events) {
                    if (e.eventId() == preselectEventId) {
                        eventCombo.setValue(e);
                        handleGenerate();
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

    private void handleGenerate() {
        EventSummary sel = eventCombo.getValue();
        if (sel == null) {
            UiComponents.showError(banner, "Please select an event first.");
            return;
        }
        try {
            AttendanceReport report = controller.generateAttendanceReport(sel.eventId());
            reportIdLabel.setText("#" + report.getReportId());
            regCountLabel.setText(String.valueOf(report.getRegisteredCount()));
            attCountLabel.setText(String.valueOf(report.getAttendedCount()));
            rateLabel.setText(report.getAttendanceRatePercent() + "%");
            generatedAtLabel.setText("Report #" + report.getReportId() + " generated & persisted at "
                    + UiComponents.formatDateTime(report.getGeneratedAt()) + " for \"" + sel.title() + "\".");

            participantTable.setItems(FXCollections.observableArrayList(report.getParticipants()));
            UiComponents.showSuccess(banner, "Saved AttendanceReport #" + report.getReportId()
                    + " (" + report.getAttendedCount() + " attended of " + report.getRegisteredCount() + " registered).");
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }
}
