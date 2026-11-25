package unievents.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import unievents.application.ManagementController;
import unievents.shared.EventDetails;
import unievents.shared.EventStatus;
import unievents.shared.EventSummary;

import java.util.List;

/**
 * UC7 (Manage Event Status — enforces valid state transitions DRAFT→PUBLISHED/CANCELLED, PUBLISHED→COMPLETED/CANCELLED).
 * Maps directly to :EventStatusUI in SD-07.
 */
public class EventStatusUI extends VBox {
    private final ManagementController controller;
    private final ComboBox<EventSummary> eventCombo = new ComboBox<>();
    private final Label currentStatusBadge = UiComponents.statusBadge("—");
    private final Label transitionHint = new Label();
    private final ComboBox<EventStatus> targetStatusCombo = new ComboBox<>();
    private final TextArea reasonArea = new TextArea();
    private final Label banner = UiComponents.createBanner();

    public EventStatusUI(ManagementController controller) {
        this.controller = controller;
        setSpacing(16);
        setPadding(new Insets(20, 24, 24, 24));
        buildView();
        refreshData(null);
    }

    private void buildView() {
        Label title = new Label("Manage Event Lifecycle Status");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("UC7 · Publish draft events to the campus catalogue, mark published events completed, or cancel with participant notification.");
        subtitle.getStyleClass().add("page-subtitle");

        eventCombo.setPrefWidth(460);
        eventCombo.valueProperty().addListener((obs, o, sel) -> inspectEvent(sel));

        targetStatusCombo.setItems(FXCollections.observableArrayList(EventStatus.values()));
        targetStatusCombo.setPrefWidth(220);

        reasonArea.setPromptText("Optional transition / cancellation reason (sent to registered & waitlisted students if CANCELLED)...");
        reasonArea.setPrefRowCount(3);

        Button applyBtn = new Button("Apply Status Transition (UC7)");
        applyBtn.getStyleClass().add("btn-primary");
        applyBtn.setOnAction(e -> handleChangeStatus());

        Button refreshBtn = new Button("Refresh My Events");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> {
            UiComponents.hideBanner(banner);
            refreshData(eventCombo.getValue() != null ? eventCombo.getValue().eventId() : null);
        });

        HBox currentRow = new HBox(12, UiComponents.fieldLabel("Current Status:"), currentStatusBadge, transitionHint);
        currentRow.setAlignment(Pos.CENTER_LEFT);
        transitionHint.setStyle("-fx-text-fill: #64748b;");

        VBox card = new VBox(12,
                UiComponents.fieldLabel("1. Select Club Event"),
                eventCombo,
                currentRow,
                new Separator(),
                UiComponents.fieldLabel("2. Select Target Status"),
                targetStatusCombo,
                UiComponents.fieldLabel("3. Reason / Notes (Optional)"),
                reasonArea,
                new HBox(10, applyBtn, refreshBtn)
        );
        card.getStyleClass().add("card");
        card.setMaxWidth(680);

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

    private void inspectEvent(EventSummary sel) {
        if (sel == null) {
            return;
        }
        EventDetails d = controller.getEventDetails(sel.eventId());
        currentStatusBadge.setText(d.status().name());
        switch (d.status()) {
            case DRAFT -> {
                transitionHint.setText("Allowed transitions: PUBLISHED or CANCELLED");
                targetStatusCombo.setValue(EventStatus.PUBLISHED);
            }
            case PUBLISHED -> {
                transitionHint.setText("Allowed transitions: COMPLETED or CANCELLED");
                targetStatusCombo.setValue(EventStatus.COMPLETED);
            }
            case COMPLETED, CANCELLED -> transitionHint.setText("Terminal state — no further transitions allowed.");
        }
    }

    private void handleChangeStatus() {
        EventSummary sel = eventCombo.getValue();
        EventStatus target = targetStatusCombo.getValue();
        if (sel == null || target == null) {
            UiComponents.showError(banner, "Please select both an event and a target status.");
            return;
        }
        if (target == EventStatus.CANCELLED) {
            boolean ok = UiComponents.confirmAction(
                    "Confirm Event Cancellation",
                    "Cancel \"" + sel.title() + "\"?",
                    "Cancelled events are removed from Browse Events and registered/waitlisted students receive an in-app notice."
            );
            if (!ok) {
                return;
            }
        }
        try {
            EventStatus updated = controller.changeEventStatus(sel.eventId(), target, reasonArea.getText());
            UiComponents.showSuccess(banner, "Event \"" + sel.title() + "\" transitioned to " + updated + ".");
            reasonArea.clear();
            refreshData(sel.eventId());
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }
}
