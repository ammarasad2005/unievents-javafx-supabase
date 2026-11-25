package unievents.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import unievents.application.ManagementController;
import unievents.application.StudentController;
import unievents.domain.Admin;
import unievents.domain.ClubOrganizer;
import unievents.domain.Student;
import unievents.domain.User;
import unievents.domain.UserRoleVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Role-based application shell built using polymorphic User methods
 * with zero instanceof checks or role-string conditionals.
 */
public class MainShellView extends BorderPane {
    private final User currentUser;
    private final VBox navItemsBox = new VBox(6);
    private final List<Button> navButtons = new ArrayList<>();

    public MainShellView(User currentUser,
                         Function<Student, StudentController> studentCtrlFactory,
                         Function<ClubOrganizer, ManagementController> organizerCtrlFactory,
                         Function<Admin, ManagementController> adminCtrlFactory,
                         Runnable onLogout) {
        this.currentUser = currentUser;
        buildTopBar(onLogout);
        buildSidebar();
        configureRoleNavigationPolymorphically(studentCtrlFactory, organizerCtrlFactory, adminCtrlFactory);
    }

    private void buildTopBar(Runnable onLogout) {
        Label welcomeLabel = new Label(currentUser.getProfileSummary());
        welcomeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label rolePill = UiComponents.statusBadge(currentUser.getRoleDisplayName());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button signOutBtn = new Button("Sign Out");
        signOutBtn.getStyleClass().add("btn-secondary");
        signOutBtn.setOnAction(e -> onLogout.run());

        HBox topBar = new HBox(12, welcomeLabel, rolePill, spacer, signOutBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("topbar");
        setTop(topBar);
    }

    private void buildSidebar() {
        Label brandTitle = new Label("UniEvents");
        brandTitle.getStyleClass().add("brand-title");
        Label brandSub = new Label("Campus Event Portal");
        brandSub.getStyleClass().add("brand-subtitle");

        Label navHeader = new Label("WORKSPACE");
        navHeader.getStyleClass().add("nav-section-label");

        VBox sidebar = new VBox(8, new VBox(2, brandTitle, brandSub), navHeader, navItemsBox);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(235);
        setLeft(sidebar);
    }

    private Button addNavItem(String label, Runnable action) {
        Button btn = new Button(label);
        btn.getStyleClass().add("nav-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            setActiveButton(btn);
            action.run();
        });
        navButtons.add(btn);
        navItemsBox.getChildren().add(btn);
        return btn;
    }

    private void setActiveButton(Button active) {
        for (Button b : navButtons) {
            b.getStyleClass().remove("nav-btn-active");
        }
        if (!active.getStyleClass().contains("nav-btn-active")) {
            active.getStyleClass().add("nav-btn-active");
        }
    }

    private void showScreen(Node screen, Button navBtn) {
        setActiveButton(navBtn);
        setCenter(screen);
    }

    private void configureRoleNavigationPolymorphically(
            Function<Student, StudentController> studentCtrlFactory,
            Function<ClubOrganizer, ManagementController> organizerCtrlFactory,
            Function<Admin, ManagementController> adminCtrlFactory) {

        currentUser.accept(new UserRoleVisitor<Void>() {
            @Override
            public Void visitStudent(Student student) {
                StudentController sc = studentCtrlFactory.apply(student);

                CheckInUI checkInUI = new CheckInUI(sc);
                ReviewEventUI reviewUI = new ReviewEventUI(sc);
                StudentNotificationsView notificationsUI = new StudentNotificationsView(sc);

                final Button[] checkInBtnRef = new Button[1];
                final Button[] reviewBtnRef = new Button[1];

                BrowseEventsUI browseUI = new BrowseEventsUI(
                        sc,
                        eventId -> {
                            checkInUI.refreshData(eventId);
                            showScreen(checkInUI, checkInBtnRef[0]);
                        },
                        eventId -> {
                            reviewUI.refreshData(eventId);
                            showScreen(reviewUI, reviewBtnRef[0]);
                        }
                );

                MyRegistrationsUI myRegsUI = new MyRegistrationsUI(
                        sc,
                        eventId -> {
                            checkInUI.refreshData(eventId);
                            showScreen(checkInUI, checkInBtnRef[0]);
                        },
                        eventId -> {
                            reviewUI.refreshData(eventId);
                            showScreen(reviewUI, reviewBtnRef[0]);
                        }
                );

                Button browseBtn = addNavItem("Browse Events", () -> {
                    browseUI.refreshData();
                    setCenter(browseUI);
                });
                addNavItem("My Registrations", () -> {
                    myRegsUI.refreshData();
                    setCenter(myRegsUI);
                });
                checkInBtnRef[0] = addNavItem("Event Check-In", () -> {
                    checkInUI.refreshData(null);
                    setCenter(checkInUI);
                });
                reviewBtnRef[0] = addNavItem("Reviews & Ratings", () -> {
                    reviewUI.refreshData(null);
                    setCenter(reviewUI);
                });
                addNavItem("Notifications", () -> {
                    notificationsUI.refreshData();
                    setCenter(notificationsUI);
                });

                showScreen(browseUI, browseBtn);
                return null;
            }

            @Override
            public Void visitOrganizer(ClubOrganizer organizer) {
                ManagementController mc = organizerCtrlFactory.apply(organizer);

                EventStatusUI statusUI = new EventStatusUI(mc);
                AnnouncementUI announcementUI = new AnnouncementUI(mc);
                AttendanceUI attendanceUI = new AttendanceUI(mc);
                AttendanceReportUI reportUI = new AttendanceReportUI(mc);

                final Button[] statusBtnRef = new Button[1];
                final Button[] annBtnRef = new Button[1];
                final Button[] attBtnRef = new Button[1];
                final Button[] repBtnRef = new Button[1];

                ManageEventUI manageUI = new ManageEventUI(
                        mc,
                        evId -> { statusUI.refreshData(evId); showScreen(statusUI, statusBtnRef[0]); },
                        evId -> { announcementUI.refreshData(evId); showScreen(announcementUI, annBtnRef[0]); },
                        evId -> { attendanceUI.refreshData(evId); showScreen(attendanceUI, attBtnRef[0]); },
                        evId -> { reportUI.refreshData(evId); showScreen(reportUI, repBtnRef[0]); }
                );

                Button manageBtn = addNavItem("Manage Events", () -> {
                    manageUI.refreshData();
                    setCenter(manageUI);
                });
                statusBtnRef[0] = addNavItem("Event Status", () -> {
                    statusUI.refreshData(null);
                    setCenter(statusUI);
                });
                annBtnRef[0] = addNavItem("Announcements", () -> {
                    announcementUI.refreshData(null);
                    setCenter(announcementUI);
                });
                attBtnRef[0] = addNavItem("Attendance Sheet", () -> {
                    attendanceUI.refreshData(null);
                    setCenter(attendanceUI);
                });
                repBtnRef[0] = addNavItem("Attendance Reports", () -> {
                    reportUI.refreshData(null);
                    setCenter(reportUI);
                });

                showScreen(manageUI, manageBtn);
                return null;
            }

            @Override
            public Void visitAdmin(Admin admin) {
                ManagementController mc = adminCtrlFactory.apply(admin);

                CreateClubUI createClubUI = new CreateClubUI(mc);
                ConflictResolutionUI conflictUI = new ConflictResolutionUI(mc);

                Button clubBtn = addNavItem("Club Directory", () -> {
                    createClubUI.refreshData();
                    setCenter(createClubUI);
                });
                addNavItem("Conflict Resolution", () -> {
                    conflictUI.refreshData();
                    setCenter(conflictUI);
                });

                showScreen(createClubUI, clubBtn);
                return null;
            }
        });
    }
}
