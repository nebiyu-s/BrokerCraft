package brokercraft.utils;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;

public final class StyleManager {

    /** How long messages stay visible before auto-clearing (seconds) */
    private static final double MSG_DURATION = 4.0;

    private StyleManager() {}

    public static void styleTable(TableView<?> table) {
        table.setFixedCellSize(40);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public static void setSuccess(Label label, String text) {
        label.getStyleClass().removeAll("message-error", "message-info");
        if (!label.getStyleClass().contains("message-success")) {
            label.getStyleClass().add("message-success");
        }
        label.setText(text);
        scheduleAutoClear(label);
    }

    public static void setError(Label label, String text) {
        label.getStyleClass().removeAll("message-success", "message-info");
        if (!label.getStyleClass().contains("message-error")) {
            label.getStyleClass().add("message-error");
        }
        label.setText(text);
        scheduleAutoClear(label);
    }

    public static void setInfo(Label label, String text) {
        label.getStyleClass().removeAll("message-success", "message-error");
        if (!label.getStyleClass().contains("message-info")) {
            label.getStyleClass().add("message-info");
        }
        label.setText(text);
        scheduleAutoClear(label);
    }

    /**
     * Schedules the label text to be cleared after MSG_DURATION seconds.
     * Uses a PauseTransition so it runs on the JavaFX thread safely.
     * Each call cancels any previous timer on the same label by storing
     * the transition in the label's properties map.
     */
    private static void scheduleAutoClear(Label label) {
        // Cancel any existing timer for this label
        Object existing = label.getProperties().get("autoClearTimer");
        if (existing instanceof PauseTransition old) {
            old.stop();
        }
        PauseTransition pause = new PauseTransition(Duration.seconds(MSG_DURATION));
        pause.setOnFinished(e -> {
            label.setText("");
            label.getStyleClass().removeAll("message-success", "message-error", "message-info");
        });
        label.getProperties().put("autoClearTimer", pause);
        pause.play();
    }

    public static String formatCurrency(double amount) {
        return String.format("%,.2f ETB", amount);
    }

    public static void styleTradeSideColumn(
            TableColumn<brokercraft.model.Transaction, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("BUY".equalsIgnoreCase(item)
                        ? "-fx-text-fill:#4ade80;-fx-font-weight:bold;"
                        : "-fx-text-fill:#f87171;-fx-font-weight:bold;");
            }
        });
    }

    public static void updateSimBadge(Label label, boolean running) {
        label.getStyleClass().removeAll("badge-sim-on", "badge-sim-off", "badge-live");
        label.getStyleClass().add("badge-live");
        if (running) {
            label.getStyleClass().add("badge-sim-on");
            label.setText("LIVE - Market Running");
        } else {
            label.getStyleClass().add("badge-sim-off");
            label.setText("STOPPED - Simulation Off");
        }
    }
}
