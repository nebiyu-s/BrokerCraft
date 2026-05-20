package brokercraft.utils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class UiClock {
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d yyyy  |  HH:mm:ss");

    private UiClock() {}

    public static Timeline bind(Label label) {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                label.setText(FMT.format(LocalDateTime.now()))));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        return timeline;
    }
}
