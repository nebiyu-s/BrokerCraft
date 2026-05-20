package brokercraft.database;

import java.sql.SQLException;

public final class Db {
    @FunctionalInterface
    public interface SqlCallable<T> {
        T call() throws SQLException;
    }

    @FunctionalInterface
    public interface SqlRunnable {
        void run() throws SQLException;
    }

    private Db() {}

    public static <T> T query(SqlCallable<T> callable) {
        try {
            return callable.call();
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    public static void execute(SqlRunnable runnable) {
        query(() -> {
            runnable.run();
            return null;
        });
    }
}
