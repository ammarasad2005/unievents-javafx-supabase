package unievents.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import unievents.application.StudentController;
import unievents.shared.EventStatus;
import unievents.shared.RegistrationStatus;
import unievents.shared.RegistrationSummary;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * UC3 (My Registrations & Cancel Registration with automatic Waitlist Promotion).
 * Maps directly to :MyRegistrationsUI in SD-03.
 */
public class MyRegistrationsUI extends VBox {
    private final StudentController controller;
    private final IntConsumer onOpenCheckIn;
    private final IntConsumer onOpenReview;

    private final TableView<RegistrationSummary> table = new TableView<>();
    private final Label banner = UiComponents.createBanner();
    private final Button cancelBtn = new Button("Cancel Registration");
    private final Button checkInBtn = new Button("Check In");
    private final Button reviewBtn = new Button("Write Review");

    public MyRegistrationsUI(StudentController controller, IntConsumer onOpenCheckIn, IntConsumer onOpenReview) {
        this.controller = controller;
        this.onOpenCheckIn = onOpenCheckIn;
        this.onOpenReview = onOpenReview;

        setSpacing(14);
        setPadding(new Insets(20, 24, 24, 24));
        buildView();
        refreshData();
    }

    private void buildView() {
        Label title = new Label("My Registrations");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Track your confirmed seats, waitlist positions, and attendance history.");
        subtitle.getStyleClass().add("page-subtitle");

        TableColumn<RegistrationSummary, String> colId = new TableColumn<>("Reg #");
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().registrationId()));
        colId.setPrefWidth(70);

        TableColumn<RegistrationSummary, String> colEvent = new TableColumn<>("Event Title");
        colEvent.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().eventTitle()));
        colEvent.setPrefWidth(230);

        TableColumn<RegistrationSummary, String> colClub = new TableColumn<>("Society");
        colClub.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().clubName()));
        colClub.setPrefWidth(145);

        TableColumn<RegistrationSummary, String> colStart = new TableColumn<>("Event Schedule");
        colStart.setCellValueFactory(c -> new SimpleStringProperty(UiComponents.formatDateTime(c.getValue().startDateTime())));
        colStart.setPrefWidth(165);

        TableColumn<RegistrationSummary, String> colVenue = new TableColumn<>("Venue");
        colVenue.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().venue()));
        colVenue.setPrefWidth(145);

        TableColumn<RegistrationSummary, String> colEvStatus = new TableColumn<>("Event State");
        colEvStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().eventStatus().name()));
        colEvStatus.setPrefWidth(110);

        TableColumn<RegistrationSummary, String> colStatus = new TableColumn<>("My Registration Status");
        colStatus.setCellValueFactory(c -> {
            RegistrationSummary r = c.getValue();
            String label = r.status() == RegistrationStatus.WAITLISTED && r.waitlistPosition() != null
                    ? "WAITLISTED (Position #" + r.waitlistPosition() + ")"
                    : r.status().name();
            return new SimpleStringProperty(label);
        });
        colStatus.setPrefWidth(185);

        table.getColumns().setAll(List.of(colId, colEvent, colClub, colStart, colVenue, colEvStatus, colStatus));
        table.setPlaceholder(new Label("You have no event registrations yet. Visit Browse Events to register!"));
        VBox.setVgrow(table, Priority.ALWAYS);

        cancelBtn.getStyleClass().add("btn-danger");
        cancelBtn.setDisable(true);
        cancelBtn.setOnAction(e -> handleCancel());

        checkInBtn.getStyleClass().add("btn-primary");
        checkInBtn.setDisable(true);
        checkInBtn.setOnAction(e -> {
            RegistrationSummary sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                onOpenCheckIn.accept(sel.eventId());
            }
        });

        reviewBtn.getStyleClass().add("btn-secondary");
        reviewBtn.setDisable(true);
        reviewBtn.setOnAction(e -> {
            RegistrationSummary sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                onOpenReview.accept(sel.eventId());
            }
        });

        Button refreshBtn = new Button("Refresh List");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> {
            UiComponents.hideBanner(banner);
            refreshData();
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, sel) -> updateActionStates(sel));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actionBar = new HBox(10, cancelBtn, checkInBtn, reviewBtn, spacer, refreshBtn);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.getStyleClass().add("card");

        getChildren().addAll(new VBox(2, title, subtitle), banner, table, actionBar);
    }

    private void updateActionStates(RegistrationSummary sel) {
        if (sel == null) {
            cancelBtn.setDisable(true);
            checkInBtn.setDisable(true);
            reviewBtn.setDisable(true);
            return;
        }
        boolean canCancel = (sel.status() == RegistrationStatus.REGISTERED || sel.status() == RegistrationStatus.WAITLISTED)
                && sel.eventStatus() != EventStatus.COMPLETED;
        cancelBtn.setDisable(!canCancel);
        checkInBtn.setDisable(sel.status() != RegistrationStatus.REGISTERED);
        reviewBtn.setDisable(sel.status() != RegistrationStatus.ATTENDED);
    }

    public void refreshData() {
        try {
            List<RegistrationSummary> items = controller.getMyRegistrations();
            table.setItems(FXCollections.observableArrayList(items));
            if (!items.isEmpty()) {
                table.getSelectionModel().selectFirst();
            }
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void handleCancel() {
        RegistrationSummary sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }
        boolean wasConfirmed = sel.status() == RegistrationStatus.REGISTERED;
        boolean ok = UiComponents.confirmAction(
                "Confirm Cancellation",
                "Cancel registration for \"" + sel.eventTitle() + "\"?",
                wasConfirmed
                        ? "Your confirmed seat will be released immediately, and the first waitlisted student (if any) will be promoted automatically."
                        : "You will be removed from the waitlist for this event."
        );
        if (!ok) {
            return;
        }

        try {
            RegistrationStatus updated = controller.cancelRegistration(sel.registrationId());
            UiComponents.showSuccess(banner, "Registration #" + sel.registrationId()
                    + " for \"" + sel.eventTitle() + "\" is now " + updated
                    + (wasConfirmed ? ". Any waitlisted student #1 was automatically promoted to REGISTERED." : "."));
            refreshData();
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }
}
