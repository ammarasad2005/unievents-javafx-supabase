package unievents.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import unievents.application.AuthController;
import unievents.domain.User;

import java.util.function.Consumer;

/**
 * Application authentication screen establishing a role-based user session.
 */
public class LoginView extends StackPane {

    private final AuthController authController;
    private final Consumer<User> onLoginSuccess;

    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label banner = UiComponents.createBanner();

    public LoginView(AuthController authController, Consumer<User> onLoginSuccess) {
        this.authController = authController;
        this.onLoginSuccess = onLoginSuccess;

        setPadding(new Insets(36));
        buildView();
    }

    private void buildView() {
        Label brand = new Label("UniEvents");
        brand.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label tagline = new Label("University Event & Club Management Portal");
        tagline.getStyleClass().add("page-subtitle");

        emailField.setPromptText("name@nuces.edu");
        emailField.setOnAction(e -> passwordField.requestFocus());

        passwordField.setPromptText("Enter your password");
        passwordField.setOnAction(e -> handleLogin());

        Button signInBtn = new Button("Sign In");
        signInBtn.getStyleClass().add("btn-primary");
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.setPadding(new Insets(10, 16, 10, 16));
        signInBtn.setOnAction(e -> handleLogin());

        Label footer = new Label("Sign in with your university student, society organizer, or administrative account.");
        footer.setWrapText(true);
        footer.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748b;");

        VBox loginCard = new VBox(14,
                new VBox(4, brand, tagline),
                banner,
                new Separator(),
                new VBox(5, UiComponents.fieldLabel("Email Address"), emailField),
                new VBox(5, UiComponents.fieldLabel("Password"), passwordField),
                signInBtn,
                new Separator(),
                footer
        );
        loginCard.getStyleClass().add("card");
        loginCard.setPrefWidth(420);
        loginCard.setMaxWidth(420);
        loginCard.setAlignment(Pos.CENTER_LEFT);

        HBox wrapper = new HBox(loginCard);
        wrapper.setAlignment(Pos.CENTER);
        getChildren().add(wrapper);
    }

    private void handleLogin() {
        try {
            User user = authController.login(emailField.getText(), passwordField.getText());
            onLoginSuccess.accept(user);
        } catch (RuntimeException ex) {
            UiComponents.showError(banner, ex.getMessage());
        }
    }
}
