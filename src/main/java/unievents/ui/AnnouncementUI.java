package unievents.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import unievents.application.ManagementController;
import unievents.domain.Announcement;
import unievents.shared.AudienceCounts;
import unievents.shared.AudienceType;
import unievents.shared.EventSummary;

import java.util.List;

/**
 * UC8 (Send Event Announcement — displays live AudienceCounts and delivers Notifications to recipients).
 * Maps directly to :AnnouncementUI in SD-08.
 */
public class AnnouncementUI extends VBox {
    private final ManagementController controller;
    private final ComboBox<EventSummary> eventCombo = new ComboBox<>();
    private final Label regCountLabel = new Label("0");
    private final Label waitCountLabel = new Label("0");
    private final Label bothCountLabel = new Label("0");
    private final ComboBox<AudienceType> audienceCombo = new ComboBox<>();
    private final TextField subjectField = new TextField();
    private final TextArea messageArea = new TextArea();
    private final Label banner = UiComponents.createBanner();

    public AnnouncementUI(ManagementController controller) {
        this.controller = controller;
        setSpacing(16);
        setPadding(new Insets(20, 24, 24, 24));
        buildView();
        refreshData(null);
    }

    private void buildView() {
        Label title = new Label("Send Event Announcement");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("UC8 · Inspect registered & waitlisted audience counts and broadcast targeted in-app notifications.");
        subtitle.getStyleClass().add("page-subtitle");

        eventCombo.setPrefWidth(460);
        eventCombo.valueProperty().addListener((obs, o, sel) -> updateCounts(sel));

        HBox metricsRow = new HBox(14,
                UiComponents.metricCard("Confirmed (REGISTERED)", regCountLabel),
                UiComponents.metricCard("Waitlisted (WAITLISTED)", waitCountLabel),
                UiComponents.metricCard("Total Combined (BOTH)", bothCountLabel)
        );

        audienceCombo.setItems(FXCollections.observableArrayList(AudienceType.values()));
        audienceCombo.setValue(AudienceType.BOTH);
        audienceCombo.setPrefWidth(220);

        subjectField.setPromptText("e.g. Venue Update / Bring Laptop & Student ID");
        messageArea.setPromptText("Write the announcement message for students...");
        messageArea.setPrefRowCount(4);

        Button sendBtn = new Button("Send Announcement & Notify Audience (UC8)");
        sendBtn.getStyleClass().add("btn-primary");
        sendBtn.setOnAction(e -> handleSend());

        Button refreshBtn = new Button("Refresh Counts");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> {
            UiComponents.hideBanner(banner);
            refreshData(eventCombo.getValue() != null ? eventCombo.getValue().eventId() : null);
        });

        VBox card = new VBox(12,
                UiComponents.fieldLabel("Target Event"),
                eventCombo,
                metricsRow,
                new Separator(),
                UiComponents.fieldLabel("Recipient Audience"),
                audienceCombo,
                UiComponents.fieldLabel("Subject *"),
                subjectField,
                UiComponents.fieldLabel("Message *"),
                messageArea,
                new HBox(10, sendBtn, refreshBtn) {{ setAlignment(Pos.CENTER_LEFT); }}
        );
        card.getStyleClass().add("card");
        card.setMaxWidth(700);

        getChildren().addAll(new VBox(2, title, subtitle), banner, card);
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

    private void updateCounts(EventSummary sel) {
        if (sel == null) {
            regCountLabel.setText("0");
            waitCountLabel.setText("0");
            bothCountLabel.setText("0");
            return;
        }
        try {
            AudienceCounts counts = controller.getAudienceCounts(sel.eventId());
            regCountLabel.setText(String.valueOf(counts.registeredCount()));
            waitCountLabel.setText(String.valueOf(counts.waitlistedCount()));
            bothCountLabel.setText(String.valueOf(counts.bothCount()));
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void handleSend() {
        EventSummary sel = eventCombo.getValue();
        if (sel == null) {
            UiComponents.showError(banner, "Please select an event first.");
            return;
        }
        try {
            Announcement sent = controller.sendAnnouncement(
                    sel.eventId(),
                    subjectField.getText(),
                    messageArea.getText(),
                    audienceCombo.getValue()
            );
            UiComponents.showSuccess(banner, "Announcement #" + sent.getAnnouncementId()
                    + " sent at " + UiComponents.formatDateTime(sent.getSentAt())
                    + " — delivered in-app notifications to " + sent.getNotifiedCount() + " student(s).");
            subjectField.clear();
            messageArea.clear();
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }
}
