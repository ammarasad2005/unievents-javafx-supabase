package unievents.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import unievents.application.StudentController;
import unievents.domain.Review;
import unievents.shared.EventDetails;
import unievents.shared.EventSummary;
import unievents.shared.RegistrationSummary;
import unievents.shared.ReviewSummary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UC5 (Rate and Review Event — verifies attendance & single-review eligibility, updates average rating).
 * Maps directly to :ReviewEventUI in SD-05.
 */
public class ReviewEventUI extends VBox {
    private final StudentController controller;
    private final ComboBox<ReviewTarget> eventCombo = new ComboBox<>();
    private final Label eligibilityBadge = UiComponents.statusBadge("Select an event");
    private final Label averageLabel = new Label("Average Rating: —");
    private final Spinner<Integer> ratingSpinner = new Spinner<>(1, 5, 5);
    private final TextArea commentArea = new TextArea();
    private final Button submitBtn = new Button("Submit Review");
    private final ListView<String> existingReviews = new ListView<>();
    private final Label banner = UiComponents.createBanner();

    private record ReviewTarget(int eventId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public ReviewEventUI(StudentController controller) {
        this.controller = controller;
        setSpacing(14);
        setPadding(new Insets(20, 24, 24, 24));
        buildView();
        refreshData(null);
    }

    private void buildView() {
        Label title = new Label("Event Reviews & Ratings");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Share feedback and rate events you have attended.");
        subtitle.getStyleClass().add("page-subtitle");

        eventCombo.setPrefWidth(460);
        eventCombo.setPromptText("Choose an event...");
        eventCombo.valueProperty().addListener((obs, o, sel) -> inspectSelectedEvent(sel));

        Button refreshBtn = new Button("Refresh Events");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> {
            UiComponents.hideBanner(banner);
            refreshData(eventCombo.getValue() != null ? eventCombo.getValue().eventId() : null);
        });

        HBox selectorRow = new HBox(12, eventCombo, refreshBtn);
        selectorRow.setAlignment(Pos.CENTER_LEFT);

        averageLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");
        HBox statusRow = new HBox(14, UiComponents.fieldLabel("Review Eligibility:"), eligibilityBadge, averageLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        ratingSpinner.setPrefWidth(100);
        commentArea.setPromptText("Share constructive feedback on speakers, organization, and venue...");
        commentArea.setPrefRowCount(4);

        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setDisable(true);
        submitBtn.setOnAction(e -> handleSubmit());

        VBox formBox = new VBox(10,
                UiComponents.fieldLabel("Select Event"),
                selectorRow,
                statusRow,
                new Separator(),
                UiComponents.fieldLabel("Star Rating (1–5)"),
                ratingSpinner,
                UiComponents.fieldLabel("Comments"),
                commentArea,
                submitBtn
        );
        formBox.getStyleClass().add("card");
        formBox.setPrefWidth(540);

        existingReviews.setPlaceholder(new Label("No reviews submitted for this event yet."));
        VBox.setVgrow(existingReviews, Priority.ALWAYS);
        VBox reviewsCard = new VBox(10, UiComponents.fieldLabel("Published Reviews for Selected Event"), existingReviews);
        reviewsCard.getStyleClass().add("card");
        HBox.setHgrow(reviewsCard, Priority.ALWAYS);

        HBox contentRow = new HBox(16, formBox, reviewsCard);
        VBox.setVgrow(contentRow, Priority.ALWAYS);

        getChildren().addAll(new VBox(2, title, subtitle), banner, contentRow);
    }

    public void refreshData(Integer preselectEventId) {
        try {
            Map<Integer, ReviewTarget> targets = new LinkedHashMap<>();
            // Include student's own registrations first (so COMPLETED attended events appear!)
            for (RegistrationSummary r : controller.getMyRegistrations()) {
                targets.put(r.eventId(), new ReviewTarget(
                        r.eventId(),
                        "#" + r.eventId() + " · " + r.eventTitle() + " [My Status: " + r.status() + "]"
                ));
            }
            for (EventSummary e : controller.getPublishedEvents()) {
                targets.putIfAbsent(e.eventId(), new ReviewTarget(
                        e.eventId(),
                        "#" + e.eventId() + " · " + e.title() + " [" + e.clubName() + "]"
                ));
            }

            List<ReviewTarget> items = List.copyOf(targets.values());
            eventCombo.setItems(FXCollections.observableArrayList(items));

            if (preselectEventId != null && targets.containsKey(preselectEventId)) {
                eventCombo.setValue(targets.get(preselectEventId));
            } else if (!items.isEmpty()) {
                eventCombo.getSelectionModel().selectFirst();
            }
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void inspectSelectedEvent(ReviewTarget target) {
        if (target == null) {
            submitBtn.setDisable(true);
            return;
        }
        try {
            boolean eligible = controller.checkReviewEligibility(target.eventId());
            EventDetails details = controller.getEventDetails(target.eventId());

            eligibilityBadge.getStyleClass().setAll("badge", eligible ? "badge-green" : "badge-amber");
            eligibilityBadge.setText(eligible ? "Eligible to Review" : "Not Eligible");
            submitBtn.setDisable(!eligible);

            averageLabel.setText(details.averageRating() > 0
                    ? "★ Average Rating: " + details.averageRating() + " / 5.0 (" + details.reviews().size() + " review(s))"
                    : "★ Average Rating: No reviews yet");

            List<String> formatted = details.reviews().stream()
                    .map((ReviewSummary r) -> "★".repeat(r.rating()) + " (" + r.rating() + "/5) · "
                            + r.studentName() + " (" + UiComponents.formatDateTime(r.postedAt()) + ")\n"
                            + (r.comment().isEmpty() ? "(No written comment)" : r.comment()))
                    .toList();
            existingReviews.setItems(FXCollections.observableArrayList(formatted));
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void handleSubmit() {
        ReviewTarget target = eventCombo.getValue();
        if (target == null) {
            return;
        }
        try {
            int rating = ratingSpinner.getValue();
            Review saved = controller.submitReview(target.eventId(), rating, commentArea.getText());
            EventDetails updated = controller.getEventDetails(target.eventId());
            UiComponents.showSuccess(banner, "Thank you! Review #" + saved.getReviewId()
                    + " (" + rating + "/5 ★) saved. Updated event average rating: " + updated.averageRating() + " / 5.0.");
            commentArea.clear();
            inspectSelectedEvent(target);
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }
}
