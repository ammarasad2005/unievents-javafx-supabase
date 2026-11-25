package unievents.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Shared pure-UI formatting and feedback helpers.
 */
public final class UiComponents {
    public static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private UiComponents() {
    }

    public static String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DT_FMT) : "—";
    }

    public static Label createBanner() {
        Label banner = new Label();
        banner.setWrapText(true);
        banner.setMaxWidth(Double.MAX_VALUE);
        banner.setVisible(false);
        banner.setManaged(false);
        return banner;
    }

    public static void showSuccess(Label banner, String message) {
        banner.getStyleClass().setAll("banner-success");
        banner.setText("✓ " + message);
        banner.setVisible(true);
        banner.setManaged(true);
    }

    public static void showError(Label banner, String message) {
        banner.getStyleClass().setAll("banner-error");
        banner.setText("⚠ " + message);
        banner.setVisible(true);
        banner.setManaged(true);
    }

    public static void showInfo(Label banner, String message) {
        banner.getStyleClass().setAll("banner-info");
        banner.setText("ℹ " + message);
        banner.setVisible(true);
        banner.setManaged(true);
    }

    public static void hideBanner(Label banner) {
        banner.setVisible(false);
        banner.setManaged(false);
    }

    public static Label statusBadge(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("badge");
        String upper = text != null ? text.toUpperCase() : "";
        if (upper.contains("PUBLISHED") || upper.contains("REGISTERED") || upper.contains("ATTENDED") || upper.contains("RESOLVED")) {
            lbl.getStyleClass().add("badge-green");
        } else if (upper.contains("WAITLIST") || upper.contains("DRAFT") || upper.contains("OPEN")) {
            lbl.getStyleClass().add("badge-amber");
        } else if (upper.contains("CANCEL") || upper.contains("BREACH") || upper.contains("OVERBOOK")) {
            lbl.getStyleClass().add("badge-red");
        } else if (upper.contains("COMPLETED")) {
            lbl.getStyleClass().add("badge-blue");
        } else {
            lbl.getStyleClass().add("badge-slate");
        }
        return lbl;
    }

    public static VBox metricCard(String label, Label valueLabel) {
        valueLabel.getStyleClass().add("metric-value");
        Label caption = new Label(label);
        caption.getStyleClass().add("metric-label");
        VBox box = new VBox(3, valueLabel, caption);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("metric-card");
        box.setPrefWidth(165);
        return box;
    }

    public static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    public static boolean confirmAction(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
