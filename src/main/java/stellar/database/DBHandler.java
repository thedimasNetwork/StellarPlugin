package stellar.database;

import arc.util.*;
import stellar.PlayerData;
import stellar.database.tables.Playtime;
import stellar.database.tables.Users;

import java.sql.*;

public class DBHandler {

    private static Connection connection;

    public static Connection getDbConnection() throws SQLException {
        if (connection == null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(Config.getConnectionUrl(), Config.DB_USER, Config.DB_PASS);
            } catch (Throwable t) {
                Log.err(t);
            }
        } else if (connection.isClosed()) {
            connection = DriverManager.getConnection(Config.getConnectionUrl(), Config.DB_USER, Config.DB_PASS);
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

    public static void save(PlayerData data) throws SQLException {
        preparedExecute("INSERT INTO " + Users.U_TABLE + " (" + Users.U_ALL + ") VALUES (" + Users.U_ALL_RAW + ")",
                data.uuid, data.ip, escapeString(data.name), data.locale, data.translator, data.admin, data.banned);
        preparedExecute("INSERT INTO " + Playtime.P_TABLE + " (" + Playtime.P_UUID + ") VALUES (?)", data.uuid);
    }

    private static String escapeString(String text) {
        return text.replace("&", "&amp").replace("\"","&quot").replace("'","&apos");
    }

    public static boolean userExist(String uuid) throws SQLException {
        String select = "SELECT * FROM " + Users.U_TABLE + " WHERE " + Users.U_UUID + "=?";
        try (PreparedStatement prSt = getDbConnection().prepareStatement(select)) {
            prSt.setString(1, uuid);
            return prSt.executeQuery().next();
        }
    }

    @Nullable
    public static <T> T get(String uuid, Field<T> column) throws SQLException {
        String select = "SELECT " + column.getName() + " FROM " + column.getTable() + " WHERE " + Users.U_UUID + "=?";
        try (PreparedStatement prSt = getDbConnection().prepareStatement(select)) {
            prSt.setString(1, uuid);

            ResultSet res = prSt.executeQuery();
            return res.next() ? res.getObject(1, column.getType()) : null;
        }
    }

    @Nullable
    public static PlayerData get(String uuid) throws SQLException {
        String select = "SELECT " + Users.U_ALL + " FROM " + Users.U_TABLE + " WHERE " + Users.U_UUID + "=?";
        try (PreparedStatement prSt = getDbConnection().prepareStatement(select)) {
            prSt.setString(1, uuid);

            ResultSet data = prSt.executeQuery();
            if (!data.next()) {
                return null;
            }

            PlayerData result = new PlayerData();
            result.uuid = data.getString(Users.U_UUID);
            result.ip = data.getString(Users.U_IP);
            result.name = data.getString(Users.U_NAME);
            result.locale = data.getString(Users.U_LOCALE);
            result.translator = data.getString(Users.U_TRANSLATOR);
            result.admin = data.getBoolean(Users.U_ADMIN);
            result.banned = data.getBoolean(Users.U_BANNED);

            return result;
        }
    }

    public static <T> void update(String uuid, Field<T> column, T value) throws SQLException {
        preparedExecute("UPDATE " + column.getTable() + " SET " + column.getName() + "=? WHERE " + Users.U_UUID + "=?",
                escapeString(String.valueOf(value)), uuid);
    }
}
