package unievents.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import unievents.application.StudentController;
import unievents.domain.Category;
import unievents.domain.Club;
import unievents.domain.Registration;
import unievents.shared.EventDetails;
import unievents.shared.EventSummary;
import unievents.shared.RegistrationStatus;
import unievents.shared.ReviewSummary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * UC1 (Browse Events + Filters + Event Details) & UC2 (Register for Event — confirmed or waitlisted).
 * Maps to :BrowseEventsUI (SD-01) and :RegisterEventUI (SD-02).
 */
public class BrowseEventsUI extends VBox {
    private final StudentController controller;
    private final IntConsumer onOpenCheckIn;
    private final IntConsumer onOpenReview;

    private final ComboBox<Category> categoryFilter = new ComboBox<>();
    private final ComboBox<Club> clubFilter = new ComboBox<>();
    private final DatePicker dateFilter = new DatePicker();
    private final TableView<EventSummary> eventsTable = new TableView<>();
    private final Label banner = UiComponents.createBanner();

    // Detail pane fields
    private final Label detailTitle = new Label("Select an event to inspect details");
    private final HBox detailBadges = new HBox(8);
    private final Label detailClubCat = new Label("");
    private final Label detailSchedule = new Label("");
    private final Label detailVenue = new Label("");
    private final Label detailSeats = new Label("");
    private final Label detailRating = new Label("");
    private final Label detailDescription = new Label("");
    private final ListView<String> reviewsList = new ListView<>();
    private final Button registerBtn = new Button("Register for Event (UC2)");
    private final Button jumpCheckInBtn = new Button("Self Check-In");
    private final Button jumpReviewBtn = new Button("Rate & Review");

    private Integer selectedEventId;

    public BrowseEventsUI(StudentController controller, IntConsumer onOpenCheckIn, IntConsumer onOpenReview) {
        this.controller = controller;
        this.onOpenCheckIn = onOpenCheckIn;
        this.onOpenReview = onOpenReview;

        setSpacing(14);
        setPadding(new Insets(20, 24, 24, 24));
        buildView();
        refreshData();
    }

    private void buildView() {
        Label title = new Label("Browse Published Events");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("UC1 & UC2 · Discover society events, filter by category/club/date, and register for a confirmed seat or waitlist spot.");
        subtitle.getStyleClass().add("page-subtitle");

        // Filter bar
        categoryFilter.setPromptText("All Categories");
        categoryFilter.setPrefWidth(175);
        clubFilter.setPromptText("All Clubs / Societies");
        clubFilter.setPrefWidth(200);
        dateFilter.setPromptText("Any Date");
        dateFilter.setPrefWidth(155);

        Button applyFilterBtn = new Button("Apply Filters");
        applyFilterBtn.getStyleClass().add("btn-primary");
        applyFilterBtn.setOnAction(e -> applyFilters());

        Button clearFilterBtn = new Button("Reset");
        clearFilterBtn.getStyleClass().add("btn-secondary");
        clearFilterBtn.setOnAction(e -> {
            categoryFilter.setValue(null);
            clubFilter.setValue(null);
            dateFilter.setValue(null);
            UiComponents.hideBanner(banner);
            loadEvents(controller.getPublishedEvents());
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refreshBtn = new Button("Refresh Catalogue");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refreshData());

        HBox filterBar = new HBox(10,
                UiComponents.fieldLabel("Category:"), categoryFilter,
                UiComponents.fieldLabel("Society:"), clubFilter,
                UiComponents.fieldLabel("Date:"), dateFilter,
                applyFilterBtn, clearFilterBtn, spacer, refreshBtn);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("card");

        // Events Table
        TableColumn<EventSummary, String> colTitle = new TableColumn<>("Event Title");
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().title()));
        colTitle.setPrefWidth(210);

        TableColumn<EventSummary, String> colClub = new TableColumn<>("Society");
        colClub.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().clubName()));
        colClub.setPrefWidth(140);

        TableColumn<EventSummary, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().categoryName()));
        colCat.setPrefWidth(100);

        TableColumn<EventSummary, String> colStart = new TableColumn<>("Start Schedule");
        colStart.setCellValueFactory(c -> new SimpleStringProperty(UiComponents.formatDateTime(c.getValue().startDateTime())));
        colStart.setPrefWidth(155);

        TableColumn<EventSummary, String> colVenue = new TableColumn<>("Venue");
        colVenue.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().venue()));
        colVenue.setPrefWidth(130);

        TableColumn<EventSummary, String> colSeats = new TableColumn<>("Seats Left");
        colSeats.setCellValueFactory(c -> {
            EventSummary ev = c.getValue();
            String label = ev.remainingSeats() > 0
                    ? ev.remainingSeats() + " / " + ev.capacity()
                    : "FULL (Waitlist: " + ev.waitlistedCount() + ")";
            return new SimpleStringProperty(label);
        });
        colSeats.setPrefWidth(135);

        eventsTable.getColumns().setAll(List.of(colTitle, colClub, colCat, colStart, colVenue, colSeats));
        eventsTable.setPlaceholder(new Label("No published events match the current filter criteria."));
        eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                openEventDetails(newSel.eventId());
            }
        });
        VBox.setVgrow(eventsTable, Priority.ALWAYS);

        // Right-hand Detail & Register Card
        detailTitle.getStyleClass().add("section-heading");
        detailTitle.setWrapText(true);
        detailDescription.setWrapText(true);
        detailDescription.setStyle("-fx-text-fill: #334155;");
        reviewsList.setPrefHeight(120);
        reviewsList.setPlaceholder(new Label("No student reviews yet."));

        registerBtn.getStyleClass().add("btn-success");
        registerBtn.setDisable(true);
        registerBtn.setOnAction(e -> handleRegister());

        jumpCheckInBtn.getStyleClass().add("btn-secondary");
        jumpCheckInBtn.setDisable(true);
        jumpCheckInBtn.setOnAction(e -> {
            if (selectedEventId != null) {
                onOpenCheckIn.accept(selectedEventId);
            }
        });

        jumpReviewBtn.getStyleClass().add("btn-secondary");
        jumpReviewBtn.setDisable(true);
        jumpReviewBtn.setOnAction(e -> {
            if (selectedEventId != null) {
                onOpenReview.accept(selectedEventId);
            }
        });

        HBox actionsBox = new HBox(10, registerBtn, jumpCheckInBtn, jumpReviewBtn);
        actionsBox.setAlignment(Pos.CENTER_LEFT);

        VBox detailCard = new VBox(10,
                detailTitle,
                detailBadges,
                detailClubCat,
                detailSchedule,
                detailVenue,
                detailSeats,
                detailRating,
                new Separator(),
                UiComponents.fieldLabel("About This Event"),
                detailDescription,
                new Separator(),
                actionsBox,
                UiComponents.fieldLabel("Recent Student Reviews"),
                reviewsList
        );
        detailCard.getStyleClass().add("card");
        detailCard.setMinWidth(360);

        SplitPane splitPane = new SplitPane(eventsTable, detailCard);
        splitPane.setDividerPositions(0.58);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        getChildren().addAll(new VBox(2, title, subtitle), banner, filterBar, splitPane);
    }

    public void refreshData() {
        try {
            Category prevCat = categoryFilter.getValue();
            Club prevClub = clubFilter.getValue();
            categoryFilter.setItems(FXCollections.observableArrayList(controller.getCategories()));
            clubFilter.setItems(FXCollections.observableArrayList(controller.getClubs()));
            categoryFilter.setValue(prevCat);
            clubFilter.setValue(prevClub);
            applyFilters();
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void applyFilters() {
        try {
            Integer catId = categoryFilter.getValue() != null ? categoryFilter.getValue().getCategoryId() : null;
            Integer clubId = clubFilter.getValue() != null ? clubFilter.getValue().getClubId() : null;
            LocalDate picked = dateFilter.getValue();
            LocalDateTime filterDate = picked != null ? picked.atStartOfDay() : null;
            List<EventSummary> filtered = controller.filterEvents(catId, clubId, filterDate);
            loadEvents(filtered);
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void loadEvents(List<EventSummary> items) {
        eventsTable.setItems(FXCollections.observableArrayList(items));
        if (!items.isEmpty()) {
            EventSummary target = items.get(0);
            if (selectedEventId != null) {
                for (EventSummary item : items) {
                    if (item.eventId() == selectedEventId) {
                        target = item;
                        break;
                    }
                }
            }
            eventsTable.getSelectionModel().select(target);
        } else {
            clearDetailPane();
        }
    }

    private void openEventDetails(int eventId) {
        try {
            EventDetails d = controller.getEventDetails(eventId);
            selectedEventId = d.eventId();
            detailTitle.setText(d.title());
            detailBadges.getChildren().setAll(
                    UiComponents.statusBadge(d.status().name()),
                    UiComponents.statusBadge(d.remainingSeats() > 0
                            ? d.remainingSeats() + " Seats Open"
                            : "At Capacity · Waitlist Active")
            );
            detailClubCat.setText("🏛 Society: " + d.clubName() + "   |   🏷 Category: " + d.categoryName());
            detailSchedule.setText("🕒 Schedule: " + UiComponents.formatDateTime(d.startDateTime())
                    + " → " + UiComponents.formatDateTime(d.endDateTime()));
            detailVenue.setText("📍 Venue: " + d.venue());
            detailSeats.setText("💺 Capacity: " + d.capacity()
                    + "   |   Confirmed: " + d.registeredCount()
                    + "   |   Remaining: " + d.remainingSeats()
                    + "   |   Waitlisted: " + d.waitlistedCount());
            detailRating.setText("★ Average Rating: "
                    + (d.averageRating() > 0 ? d.averageRating() + " / 5.0 (" + d.reviews().size() + " review(s))" : "No ratings yet"));
            detailDescription.setText(d.description().isEmpty() ? "No additional description provided." : d.description());

            List<String> formattedReviews = d.reviews().stream()
                    .map((ReviewSummary r) -> "★".repeat(r.rating()) + " (" + r.rating() + "/5) — "
                            + r.studentName() + ": " + r.comment())
                    .toList();
            reviewsList.setItems(FXCollections.observableArrayList(formattedReviews));

            registerBtn.setDisable(false);
            registerBtn.setText(d.remainingSeats() > 0 ? "Register Seat (UC2)" : "Join Waitlist (UC2)");
            jumpCheckInBtn.setDisable(false);
            jumpReviewBtn.setDisable(false);
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void handleRegister() {
        if (selectedEventId == null) {
            return;
        }
        try {
            Registration reg = controller.registerForEvent(selectedEventId);
            if (reg.getStatus() == RegistrationStatus.REGISTERED) {
                UiComponents.showSuccess(banner, "Confirmed! You are REGISTERED for \""
                        + reg.getEvent().getTitle() + "\" (Registration #" + reg.getRegistrationId() + ").");
            } else {
                UiComponents.showInfo(banner, "Event is at full capacity — you have been placed on the WAITLIST at Position #"
                        + reg.getWaitlistPosition() + " for \"" + reg.getEvent().getTitle() + "\".");
            }
            applyFilters();
            openEventDetails(selectedEventId);
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void clearDetailPane() {
        selectedEventId = null;
        detailTitle.setText("Select an event to inspect details");
        detailBadges.getChildren().clear();
        detailClubCat.setText("");
        detailSchedule.setText("");
        detailVenue.setText("");
        detailSeats.setText("");
        detailRating.setText("");
        detailDescription.setText("");
        reviewsList.getItems().clear();
        registerBtn.setDisable(true);
        jumpCheckInBtn.setDisable(true);
        jumpReviewBtn.setDisable(true);
    }
}
