package unievents.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import unievents.application.AuthController;
import unievents.domain.User;

import java.util.function.Consumer;

/**
 * Thin session login screen (precondition for role-based dashboards) with quick-fill demo pills
 * for rapid 10-minute demonstration of all 12 Use Cases across Student, Organizer, and Admin roles.
 */
public class LoginView extends StackPane {

    public interface DbConnectionConfigurer {
        boolean isPlaceholder();
        String currentUrl();
        String currentUser();
        void apply(String url, String user, String password);
    }

    private final AuthController authController;
    private final DbConnectionConfigurer dbConfigurer;
    private final Consumer<User> onLoginSuccess;

    private final TextField emailField = new TextField("ali.raza@nuces.edu");
    private final PasswordField passwordField = new PasswordField();
    private final Label banner = UiComponents.createBanner();

    public LoginView(AuthController authController,
                     DbConnectionConfigurer dbConfigurer,
                     Consumer<User> onLoginSuccess) {
        this.authController = authController;
        this.dbConfigurer = dbConfigurer;
        this.onLoginSuccess = onLoginSuccess;

        setPadding(new Insets(32));
        buildView();
    }

    private void buildView() {
        Label brand = new Label("UniEvents");
        brand.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label tagline = new Label("University Event & Club Management System · FAST-NUCES SDA");
        tagline.getStyleClass().add("page-subtitle");

        passwordField.setText("demo123");
        emailField.setPromptText("Enter campus email...");
        passwordField.setPromptText("Enter password...");
        passwordField.setOnAction(e -> handleLogin());

        Button signInBtn = new Button("Sign In to UniEvents");
        signInBtn.getStyleClass().add("btn-primary");
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.setOnAction(e -> handleLogin());

        // Quick-fill demo pills matching live Supabase seed accounts
        FlowPane demoPills = new FlowPane(8, 8);
        demoPills.getChildren().addAll(
                quickFillBtn("Student: Ali Raza", "ali.raza@nuces.edu"),
                quickFillBtn("Student: Fatima Noor", "fatima.noor@nuces.edu"),
                quickFillBtn("Organizer: Hamza (ACM)", "hamza.tariq@nuces.edu"),
                quickFillBtn("Organizer: Sara (Sports)", "sara.malik@nuces.edu"),
                quickFillBtn("Admin: Dr. Ayesha Khan", "ayesha.khan@unievents.edu")
        );

        // Collapsible DB Connection Settings panel (auto-expanded if db.password is still placeholder)
        TextField urlField = new TextField(dbConfigurer.currentUrl());
        TextField dbUserField = new TextField(dbConfigurer.currentUser());
        PasswordField dbPassField = new PasswordField();
        dbPassField.setPromptText("Paste your Supabase Database Password");

        Button saveDbBtn = new Button("Apply JDBC Credentials");
        saveDbBtn.getStyleClass().add("btn-secondary");
        saveDbBtn.setOnAction(e -> {
            dbConfigurer.apply(urlField.getText(), dbUserField.getText(), dbPassField.getText());
            UiComponents.showInfo(banner, "Updated runtime JDBC credentials. Click 'Sign In to UniEvents' to connect.");
        });

        VBox dbBox = new VBox(8,
                UiComponents.fieldLabel("JDBC URL (Supabase IPv4 Session Pooler)"),
                urlField,
                UiComponents.fieldLabel("Database User"),
                dbUserField,
                UiComponents.fieldLabel("Database Password"),
                dbPassField,
                saveDbBtn
        );
        TitledPane dbPane = new TitledPane("Supabase PostgreSQL JDBC Connection Settings", dbBox);
        dbPane.setExpanded(dbConfigurer.isPlaceholder());

        if (dbConfigurer.isPlaceholder()) {
            UiComponents.showInfo(banner,
                    "Supabase password placeholder detected in db.properties. Paste your Supabase DB password below (or set UNIEVENTS_DB_PASSWORD) and click Sign In.");
        }

        VBox loginCard = new VBox(12,
                new VBox(3, brand, tagline),
                banner,
                new Separator(),
                UiComponents.fieldLabel("Campus Email"),
                emailField,
                UiComponents.fieldLabel("Password"),
                passwordField,
                signInBtn,
                new Separator(),
                UiComponents.fieldLabel("Quick-Fill Demo Accounts (password: demo123)"),
                demoPills,
                dbPane
        );
        loginCard.getStyleClass().add("card");
        loginCard.setMaxWidth(540);
        loginCard.setAlignment(Pos.CENTER_LEFT);

        HBox wrapper = new HBox(loginCard);
        wrapper.setAlignment(Pos.CENTER);
        getChildren().add(wrapper);
    }

    private Button quickFillBtn(String label, String email) {
        Button b = new Button(label);
        b.getStyleClass().add("btn-secondary");
        b.setStyle("-fx-font-size: 11.5px; -fx-padding: 5 10 5 10;");
        b.setOnAction(e -> {
            emailField.setText(email);
            passwordField.setText("demo123");
            UiComponents.hideBanner(banner);
        });
        return b;
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
