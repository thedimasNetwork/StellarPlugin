package stellar.database;

import arc.util.Log;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.sql.*;

import static stellar.Variables.config;

public class Database {
    private static Connection connection;
    private static DSLContext context;

    private static String getConnectionUrl() {
        return "jdbc:mysql://" + config.database.ip + ":" + config.database.port + "/" + config.database.name + "?autoReconnect=true";
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(getConnectionUrl(), config.database.user, config.database.password);
            } catch (Throwable t) {
                Log.err(t);
            }
        } else if (connection.isClosed()) {
            connection = DriverManager.getConnection(getConnectionUrl(), config.database.user, config.database.password);
        }

        try { // checking if the connection is alive; may be not closed but timeout
            PreparedStatement ps = connection.prepareStatement("SELECT 1");
            ps.executeQuery();
        } catch (CommunicationsException e) {
            Log.warn("Database connection died due to inactivity");
            connection = DriverManager.getConnection(getConnectionUrl(), config.database.user, config.database.password);
        }

        return connection;
    }

    public static DSLContext getContext() throws SQLException {
        if (context == null) {
            context = DSL.using(getConnection(), SQLDialect.MYSQL);
        }
        return context;
    }
}
