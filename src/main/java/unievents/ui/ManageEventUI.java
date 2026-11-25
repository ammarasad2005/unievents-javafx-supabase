package unievents.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import unievents.application.ManagementController;
import unievents.domain.Category;
import unievents.domain.Event;
import unievents.shared.EventDetails;
import unievents.shared.EventSummary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Organizer My Events hub + UC6 (Create DRAFT Event / Edit Event Details with capacity guard).
 * Maps directly to :ManageEventUI in SD-06.
 */
public class ManageEventUI extends VBox {
    private final ManagementController controller;
    private final IntConsumer onOpenStatus;
    private final IntConsumer onOpenAnnouncement;
    private final IntConsumer onOpenAttendance;
    private final IntConsumer onOpenReport;

    private final TableView<EventSummary> eventsTable = new TableView<>();
    private final Label banner = UiComponents.createBanner();

    private Integer editingEventId = null;
    private final Label formHeading = new Label("Create New Draft Event (UC6)");
    private final Label checkInCodeLabel = new Label("Check-In Code: Auto-generated upon creation");
    private final TextField titleField = new TextField();
    private final ComboBox<Category> categoryCombo = new ComboBox<>();
    private final TextField venueField = new TextField();
    private final Spinner<Integer> capacitySpinner = new Spinner<>(1, 5000, 50);
    private final DatePicker startDatePicker = new DatePicker(LocalDate.now().plusDays(7));
    private final TextField startTimeField = new TextField("10:00");
    private final DatePicker endDatePicker = new DatePicker(LocalDate.now().plusDays(7));
    private final TextField endTimeField = new TextField("13:00");
    private final TextArea descriptionArea = new TextArea();

    public ManageEventUI(ManagementController controller,
                         IntConsumer onOpenStatus,
                         IntConsumer onOpenAnnouncement,
                         IntConsumer onOpenAttendance,
                         IntConsumer onOpenReport) {
        this.controller = controller;
        this.onOpenStatus = onOpenStatus;
        this.onOpenAnnouncement = onOpenAnnouncement;
        this.onOpenAttendance = onOpenAttendance;
        this.onOpenReport = onOpenReport;

        setSpacing(14);
        setPadding(new Insets(20, 24, 24, 24));
        buildView();
        refreshData();
    }

    private void buildView() {
        Label title = new Label("My Club Events & Event Editor");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("UC6 · Create new society events in DRAFT status or edit mutable fields. Capacity can never drop below confirmed registrations.");
        subtitle.getStyleClass().add("page-subtitle");

        // Left side: My Events table + quick jump buttons to UC7–UC10
        TableColumn<EventSummary, String> colId = new TableColumn<>("#");
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().eventId())));
        colId.setPrefWidth(45);

        TableColumn<EventSummary, String> colTitle = new TableColumn<>("Event Title");
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().title()));
        colTitle.setPrefWidth(190);

        TableColumn<EventSummary, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status().name()));
        colStatus.setPrefWidth(100);

        TableColumn<EventSummary, String> colStart = new TableColumn<>("Start");
        colStart.setCellValueFactory(c -> new SimpleStringProperty(UiComponents.formatDateTime(c.getValue().startDateTime())));
        colStart.setPrefWidth(145);

        TableColumn<EventSummary, String> colCap = new TableColumn<>("Reg / Cap");
        colCap.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().registeredCount() + " / " + c.getValue().capacity()
                        + (c.getValue().waitlistedCount() > 0 ? " (+" + c.getValue().waitlistedCount() + " WL)" : "")
        ));
        colCap.setPrefWidth(115);

        eventsTable.getColumns().setAll(List.of(colId, colTitle, colStatus, colStart, colCap));
        eventsTable.setPlaceholder(new Label("No events created for your club yet."));
        VBox.setVgrow(eventsTable, Priority.ALWAYS);

        eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) {
                populateFormForEdit(sel.eventId());
            }
        });

        Button newDraftBtn = new Button("+ New Draft Event");
        newDraftBtn.getStyleClass().add("btn-primary");
        newDraftBtn.setOnAction(e -> resetFormToCreate());

        Button statusShortcut = new Button("Status (UC7)");
        statusShortcut.getStyleClass().add("btn-secondary");
        statusShortcut.setOnAction(e -> jumpWithSelected(onOpenStatus));

        Button announceShortcut = new Button("Announce (UC8)");
        announceShortcut.getStyleClass().add("btn-secondary");
        announceShortcut.setOnAction(e -> jumpWithSelected(onOpenAnnouncement));

        Button attendanceShortcut = new Button("Attendance (UC9)");
        attendanceShortcut.getStyleClass().add("btn-secondary");
        attendanceShortcut.setOnAction(e -> jumpWithSelected(onOpenAttendance));

        Button reportShortcut = new Button("Report (UC10)");
        reportShortcut.getStyleClass().add("btn-secondary");
        reportShortcut.setOnAction(e -> jumpWithSelected(onOpenReport));

        HBox quickBar = new HBox(8, newDraftBtn, statusShortcut, announceShortcut, attendanceShortcut, reportShortcut);
        quickBar.setAlignment(Pos.CENTER_LEFT);

        VBox leftBox = new VBox(10, quickBar, eventsTable);

        // Right side: Create / Edit Form
        formHeading.getStyleClass().add("section-heading");
        checkInCodeLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");

        titleField.setPromptText("Event title");
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        venueField.setPromptText("e.g. CS Auditorium, Seminar Hall A");
        capacitySpinner.setEditable(true);
        capacitySpinner.setPrefWidth(130);

        startTimeField.setPrefWidth(85);
        endTimeField.setPrefWidth(85);
        HBox startRow = new HBox(8, startDatePicker, startTimeField);
        HBox endRow = new HBox(8, endDatePicker, endTimeField);

        descriptionArea.setPrefRowCount(3);
        descriptionArea.setPromptText("Agenda, prerequisites, speaker highlights...");

        Button saveBtn = new Button("Save Event Details (UC6)");
        saveBtn.getStyleClass().add("btn-success");
        saveBtn.setOnAction(e -> handleSave());

        Button clearBtn = new Button("Reset to Create Mode");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setOnAction(e -> resetFormToCreate());

        HBox formButtons = new HBox(10, saveBtn, clearBtn);

        VBox formCard = new VBox(8,
                formHeading,
                checkInCodeLabel,
                new Separator(),
                UiComponents.fieldLabel("Event Title *"),
                titleField,
                new HBox(12,
                        new VBox(4, UiComponents.fieldLabel("Category *"), categoryCombo),
                        new VBox(4, UiComponents.fieldLabel("Seat Capacity *"), capacitySpinner)
                ),
                UiComponents.fieldLabel("Venue *"),
                venueField,
                new HBox(16,
                        new VBox(4, UiComponents.fieldLabel("Start Date & Time (HH:mm) *"), startRow),
                        new VBox(4, UiComponents.fieldLabel("End Date & Time (HH:mm) *"), endRow)
                ),
                UiComponents.fieldLabel("Description"),
                descriptionArea,
                formButtons
        );
        formCard.getStyleClass().add("card");
        formCard.setMinWidth(410);

        SplitPane splitPane = new SplitPane(leftBox, formCard);
        splitPane.setDividerPositions(0.54);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        getChildren().addAll(new VBox(2, title, subtitle), banner, splitPane);
    }

    private void jumpWithSelected(IntConsumer consumer) {
        EventSummary sel = eventsTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            consumer.accept(sel.eventId());
        } else {
            UiComponents.showInfo(banner, "Select an event in the table first.");
        }
    }

    public void refreshData() {
        try {
            List<Category> cats = controller.getCategories();
            categoryCombo.setItems(FXCollections.observableArrayList(cats));
            if (!cats.isEmpty() && categoryCombo.getValue() == null) {
                categoryCombo.getSelectionModel().selectFirst();
            }
            List<EventSummary> myEvents = controller.getMyEvents();
            eventsTable.setItems(FXCollections.observableArrayList(myEvents));
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void resetFormToCreate() {
        eventsTable.getSelectionModel().clearSelection();
        editingEventId = null;
        formHeading.setText("Create New Draft Event (UC6)");
        checkInCodeLabel.setText("Check-In Code: Auto-generated upon creation");
        titleField.clear();
        venueField.clear();
        capacitySpinner.getValueFactory().setValue(50);
        startDatePicker.setValue(LocalDate.now().plusDays(7));
        startTimeField.setText("10:00");
        endDatePicker.setValue(LocalDate.now().plusDays(7));
        endTimeField.setText("13:00");
        descriptionArea.clear();
        if (!categoryCombo.getItems().isEmpty()) {
            categoryCombo.getSelectionModel().selectFirst();
        }
    }

    private void populateFormForEdit(int eventId) {
        try {
            EventDetails d = controller.getEventDetails(eventId);
            editingEventId = d.eventId();
            formHeading.setText("Edit Event #" + d.eventId() + " (" + d.status() + ")");
            checkInCodeLabel.setText("Venue Self Check-In Code: " + d.checkInCode()
                    + "   |   Confirmed Seats: " + d.registeredCount());
            titleField.setText(d.title());
            venueField.setText(d.venue());
            capacitySpinner.getValueFactory().setValue(d.capacity());
            startDatePicker.setValue(d.startDateTime().toLocalDate());
            startTimeField.setText(d.startDateTime().toLocalTime().format(UiComponents.TIME_FMT));
            endDatePicker.setValue(d.endDateTime().toLocalDate());
            endTimeField.setText(d.endDateTime().toLocalTime().format(UiComponents.TIME_FMT));
            descriptionArea.setText(d.description());
            for (Category c : categoryCombo.getItems()) {
                if (c.getCategoryId() == d.categoryId()) {
                    categoryCombo.setValue(c);
                    break;
                }
            }
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }

    private void handleSave() {
        try {
            Category cat = categoryCombo.getValue();
            if (cat == null) {
                UiComponents.showError(banner, "Please select an event category.");
                return;
            }
            if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                UiComponents.showError(banner, "Please select both start and end dates.");
                return;
            }
            LocalTime startTime = LocalTime.parse(startTimeField.getText().trim());
            LocalTime endTime = LocalTime.parse(endTimeField.getText().trim());
            LocalDateTime startDt = LocalDateTime.of(startDatePicker.getValue(), startTime);
            LocalDateTime endDt = LocalDateTime.of(endDatePicker.getValue(), endTime);

            capacitySpinner.increment(0); // commit any typed spinner text
            int capacity = capacitySpinner.getValue();

            boolean wasCreate = (editingEventId == null);
            Event saved = controller.saveEvent(
                    editingEventId,
                    titleField.getText(),
                    descriptionArea.getText(),
                    startDt,
                    endDt,
                    venueField.getText(),
                    capacity,
                    cat.getCategoryId()
            );

            UiComponents.showSuccess(banner, (wasCreate ? "Created new DRAFT event #" : "Updated event #")
                    + saved.getEventId() + " \"" + saved.getTitle()
                    + "\" (Check-In Code: " + saved.getCheckInCode() + ").");
            refreshData();
            populateFormForEdit(saved.getEventId());
        } catch (DateTimeParseException dtEx) {
            UiComponents.showError(banner, "Invalid time format. Use 24-hour HH:mm (for example, 14:30).");
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }
}
