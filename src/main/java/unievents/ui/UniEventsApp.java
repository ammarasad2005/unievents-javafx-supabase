package unievents.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import unievents.application.AuthController;
import unievents.application.ManagementController;
import unievents.application.StudentController;
import unievents.domain.User;
import unievents.infrastructure.persistence.DataHandler;
import unievents.infrastructure.persistence.DatabaseController;
import unievents.infrastructure.persistence.SupabaseHandler;

import java.net.URL;

/**
 * JavaFX Application Composition Root for UniEvents.
 */
public class UniEventsApp extends Application {
    private DataHandler dataHandler;
    private AuthController authController;
    private StackPane rootContainer;

    @Override
    public void start(Stage primaryStage) {
        DatabaseController db = DatabaseController.getInstance();
        this.dataHandler = new SupabaseHandler(db);
        this.authController = new AuthController(dataHandler);
        this.rootContainer = new StackPane();

        Scene scene = new Scene(rootContainer, 1240, 780);
        URL cssUrl = UniEventsApp.class.getResource("/styles/unievents.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        showLoginView();

        primaryStage.setTitle("UniEvents — University Event & Club Management");
        primaryStage.setMinWidth(1040);
        primaryStage.setMinHeight(680);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> DatabaseController.getInstance().closeConnection());
        primaryStage.show();
    }

    private void showLoginView() {
        LoginView loginView = new LoginView(authController, this::showMainShell);
        rootContainer.getChildren().setAll(loginView);
    }

    private void showMainShell(User authenticatedUser) {
        MainShellView shell = new MainShellView(
                authenticatedUser,
                student -> new StudentController(dataHandler, student),
                organizer -> ManagementController.forOrganizer(dataHandler, organizer),
                admin -> ManagementController.forAdmin(dataHandler, admin),
                () -> {
                    authController.logout();
                    showLoginView();
                }
        );
        rootContainer.getChildren().setAll(shell);
    }

    @Override
    public void stop() {
        DatabaseController.getInstance().closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
