package utils;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Backward-compatible facade around the existing database singleton.
 */
public final class MyDatabase {

    private static final MyDatabase INSTANCE = new MyDatabase();
    private final com.esprit.utils.MyDataBase delegate;

    private MyDatabase() {
        this.delegate = com.esprit.utils.MyDataBase.getInstance();
    }

    public static MyDatabase getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() {
        return delegate.getConnection();
    }

    public Connection getConnectionOrThrow() throws SQLException {
        Connection connection = getConnection();
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is not available.");
        }
        return connection;
    }
}
