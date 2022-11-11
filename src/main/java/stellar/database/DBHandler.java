package stellar.database;

import arc.util.Log;
import arc.util.Nullable;

import stellar.database.entries.PlayerEntry;
import stellar.database.tables.Playtime;
import stellar.database.tables.Tables;
import stellar.database.tables.Users;
import stellar.database.types.Entry;
import stellar.database.types.Field;
import stellar.database.types.Table;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import static stellar.Variables.config;

public class DBHandler {

    private static Connection connection;

    private static String getConnectionUrl() {
        return "jdbc:mysql://" + config.database.ip + ":" + config.database.port + "/" + config.database.name;
    }

    public static Connection getDbConnection() throws SQLException {

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

        return connection;
    }

    // для простых методов на вставку/обновление
    public static void preparedExecute(String sql, Object... values) throws SQLException {
        try (PreparedStatement stmt = getDbConnection().prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                stmt.setObject(i + 1, values[i]);
            }

            stmt.executeUpdate();
        }
    }

    public static void save(Entry data) throws SQLException {

    }

    private static String escapeString(String text) {
        return text.replace("&", "&amp").replace("\"", "&quot").replace("'", "&apos");
    }

    public static boolean userExist(String uuid) throws SQLException {
        String select = "SELECT * FROM " + Tables.users.title + " WHERE " + Tables.users.key.getName() + "=?";
        try (PreparedStatement prSt = getDbConnection().prepareStatement(select)) {
            prSt.setString(1, uuid);
            return prSt.executeQuery().next();
        }
    }

    @Nullable
    public static <T> T get(String uuid, Field<T> column, Table from) throws SQLException {
        String select = "SELECT " + column.getName() + " FROM " + column.getTable() + " WHERE " + from.key.getName() + "=?";
        try (PreparedStatement prSt = getDbConnection().prepareStatement(select)) {
            prSt.setString(1, uuid);

            ResultSet res = prSt.executeQuery();
            return res.next() ? res.getObject(1, column.getType()) : null;
        }
    }

    @Nullable
    public static <T extends Entry> T get(String key, Table from, Class<T> type) throws SQLException {
        String select = "SELECT " + from.all + " FROM " + from.title + " WHERE " + from.key + "=?";
        try (PreparedStatement prSt = getDbConnection().prepareStatement(select)) {
            prSt.setString(1, key);

            ResultSet data = prSt.executeQuery();
            if (!data.next()) {
                return null;
            }

            String[] cols = from.all.split(",");
            String[] args = new String[cols.length];
            for (int i = 0; i < cols.length; i++) {
                args[i] = data.getString(cols[i]);
            }
            return type.getDeclaredConstructor(type).newInstance(args);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> void update(String key, Field<T> column, Table where, T value) throws SQLException {
        preparedExecute("UPDATE " + where.title + " SET " + column.getName() + "=? WHERE " + where.key.getName() + "=?",
                value instanceof Boolean ? value : escapeString(String.valueOf(value)), key);
    }
}
