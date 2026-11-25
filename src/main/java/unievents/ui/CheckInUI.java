package unievents.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import unievents.application.StudentController;
import unievents.domain.Attendance;
import unievents.shared.RegistrationStatus;
import unievents.shared.RegistrationSummary;

import java.util.List;

/**
 * UC4 (Check In at Event via Check-In Code).
 * Maps directly to :CheckInUI in SD-04.
 */
public class CheckInUI extends VBox {
    private final StudentController controller;
    private final ComboBox<EventOption> eventCombo = new ComboBox<>();
    private final TextField codeField = new TextField();
    private final Label statusHint = new Label();
    private final Label banner = UiComponents.createBanner();

    private record EventOption(int eventId, String label, RegistrationStatus status, String venue) {
        @Override
        public String toString() {
            return label;
        }
    }

    public CheckInUI(StudentController controller) {
        this.controller = controller;
        setSpacing(16);
        setPadding(new Insets(20, 24, 24, 24));
        buildView();
        refreshData(null);
    }

    private void buildView() {
        Label title = new Label("Self Check-In at Event");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("UC4 · Select your registered event and enter the check-in code announced at the venue.");
        subtitle.getStyleClass().add("page-subtitle");

        eventCombo.setPrefWidth(460);
        eventCombo.setPromptText("Select one of your registered events...");
        eventCombo.valueProperty().addListener((obs, o, sel) -> {
            if (sel != null) {
                statusHint.setText("Current Registration Status: " + sel.status() + "   |   Venue: " + sel.venue());
            } else {
                statusHint.setText("");
            }
        });

        codeField.setPromptText("e.g. ACM-DEVHOUR-26");
        codeField.setMaxWidth(320);

        Button checkInBtn = new Button("Verify Code & Check In (UC4)");
        checkInBtn.getStyleClass().add("btn-success");
        checkInBtn.setOnAction(e -> handleCheckIn());

        Button refreshBtn = new Button("Refresh My Events");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> {
            UiComponents.hideBanner(banner);
            refreshData(null);
        });

        HBox buttons = new HBox(10, checkInBtn, refreshBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        statusHint.setStyle("-fx-text-fill: #475569;");

        VBox formCard = new VBox(12,
                UiComponents.fieldLabel("1. Choose Registered Event"),
                eventCombo,
                statusHint,
                UiComponents.fieldLabel("2. Enter Event Check-In Code"),
                codeField,
                buttons
        );
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(640);

        getChildren().addAll(new VBox(2, title, subtitle), banner, formCard);
    }

    public void refreshData(Integer preselectEventId) {
        try {
            List<RegistrationSummary> regs = controller.getMyRegistrations();
            List<EventOption> options = regs.stream()
                    .filter(r -> r.status() == RegistrationStatus.REGISTERED || r.status() == RegistrationStatus.ATTENDED)
                    .map(r -> new EventOption(
                            r.eventId(),
                            "#" + r.eventId() + " · " + r.eventTitle() + " [" + r.status() + "]",
                            r.status(),
                            r.venue()
                    ))
                    .toList();
            eventCombo.setItems(FXCollections.observableArrayList(options));
            if (preselectEventId != null) {
                for (EventOption opt : options) {
                    if (opt.eventId() == preselectEventId) {
                        eventCombo.setValue(opt);
                        return;
                    }
                }
            }
            if (!options.isEmpty()) {
                eventCombo.getSelectionModel().selectFirst();
            }
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void handleCheckIn() {
        EventOption sel = eventCombo.getValue();
        if (sel == null) {
            UiComponents.showError(banner, "Please select one of your registered events first.");
            return;
        }
        try {
            Attendance att = controller.checkInToEvent(sel.eventId(), codeField.getText());
            UiComponents.showSuccess(banner, "Checked in! Attendance #" + att.getAttendanceId()
                    + " recorded at " + UiComponents.formatDateTime(att.getCheckedInAt())
                    + " via method " + att.getMethod() + ". Status updated to ATTENDED.");
            codeField.clear();
            refreshData(sel.eventId());
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }
}
