package database;

import arc.util.Log;
import main.PlayerData;

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
        preparedExecute("INSERT INTO " + Const.U_TABLE + " (" + Const.U_ALL + ") VALUES (" + Const.U_ALL_RAW + ")",
                data.uuid, data.ip, escapeString(data.name), data.locale, data.translator, data.playTime, data.admin, data.banned);
    }

    private static String escapeString(String text) {
        return text.replace("&", "&amp").replace("\"","&quot").replace("'","&apos");
    }

    public static boolean userExist(String uuid) throws SQLException {
        String select = "SELECT * FROM " + Const.U_TABLE + " WHERE " + Const.U_UUID + "=?";
        try (PreparedStatement prSt = getDbConnection().prepareStatement(select)) {
            prSt.setString(1, uuid);
            return prSt.executeQuery().next();
        }
    }

    public static String get(String uuid, String column) throws SQLException {
        String select = "SELECT " + column + " FROM " + Const.U_TABLE + " WHERE " + Const.U_UUID + "=?";
        try (PreparedStatement prSt = getDbConnection().prepareStatement(select)) {
            prSt.setString(1, uuid);

            ResultSet res = prSt.executeQuery();
            return res.next() ? res.getString(1) : null;
        }
    }

    public static <T> T get(String uuid, String column, Class<T> type) throws SQLException {
        String select = "SELECT " + column + " FROM " + Const.U_TABLE + " WHERE " + Const.U_UUID + "=?";
        try (PreparedStatement prSt = getDbConnection().prepareStatement(select)) {
            prSt.setString(1, uuid);

            ResultSet res = prSt.executeQuery();
            return res.next() ? res.getObject(1, type) : null;
        }
    }

    public static PlayerData get(String uuid) throws SQLException {
        String select = "SELECT " + Const.U_ALL + " FROM " + Const.U_TABLE + " WHERE " + Const.U_UUID + "=?";
        try (PreparedStatement prSt = getDbConnection().prepareStatement(select)) {
            prSt.setString(1, uuid);

            ResultSet data = prSt.executeQuery();
            data.next();

            PlayerData result = new PlayerData();
            result.uuid = data.getString(1);
            result.ip = data.getString(2);
            result.name = data.getString(3);
            result.locale = data.getString(4);
            result.translator = data.getString(5);
            result.playTime = data.getLong(6);
            result.admin = data.getBoolean(7);
            result.banned = data.getBoolean(8);

            return result;
        }
    }

    public static void update(String uuid, String column, String value) throws SQLException {
        preparedExecute("UPDATE " + Const.U_TABLE + " SET " + column + "=? WHERE " + Const.U_UUID + "=?",
                escapeString(value), uuid);
    }
}
