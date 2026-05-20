package brokercraft.utils;

import brokercraft.model.Transaction;
import brokercraft.model.TransactionType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TransactionLogger {
    private static final Path LOG_DIR = Path.of("logs");
    private static final Path LOG_FILE = LOG_DIR.resolve("transactions.log");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    private TransactionLogger() {}

    public static void log(Transaction tx) {
        try {
            Files.createDirectories(LOG_DIR);
            String action = tx.getType() == TransactionType.BUY ? "bought" : "sold";
            String line = String.format("[%s] %s %s %d %s shares at %.2f ETB (total %.2f ETB)%n",
                    LocalDateTime.now().format(TIME_FMT),
                    tx.getClientName(),
                    action,
                    tx.getQuantity(),
                    tx.getSymbol(),
                    tx.getPrice(),
                    tx.getTotal());
            Files.writeString(LOG_FILE, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write transaction log: " + e.getMessage());
        }
    }
}
