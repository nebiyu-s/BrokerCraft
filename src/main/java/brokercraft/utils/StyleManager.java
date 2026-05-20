package brokercraft.utils;

import brokercraft.model.TransactionType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class StyleManager {
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
    }

    public static void setError(Label label, String text) {
        label.getStyleClass().removeAll("message-success", "message-info");
        if (!label.getStyleClass().contains("message-error")) {
            label.getStyleClass().add("message-error");
        }
        label.setText(text);
    }

    public static void setInfo(Label label, String text) {
        label.getStyleClass().removeAll("message-success", "message-error");
        if (!label.getStyleClass().contains("message-info")) {
            label.getStyleClass().add("message-info");
        }
        label.setText(text);
    }

    public static String formatCurrency(double amount) {
        return String.format("%,.2f ETB", amount);
    }

    public static void styleTradeSideColumn(TableColumn<brokercraft.model.Transaction, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                if ("BUY".equalsIgnoreCase(item)) {
                    setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
                }
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
