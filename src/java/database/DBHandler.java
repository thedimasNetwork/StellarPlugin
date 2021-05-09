package java.database;

import arc.util.Log;
import java.main.PlayerData;

import java.sql.*;

public class DBHandler {

    private static Connection connection;

    public static Connection getDbConnection() {
        if (connection == null) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                String connectionURL = "jdbc:mysql://" + Config.DB_HOST + ":" + Config.DB_PORT + "/" + Config.DB_NAME;
                connection = DriverManager.getConnection(connectionURL, Config.DB_USER, Config.DB_PASS);
            } catch (Throwable t) {
                Log.err(t);
            }
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
        preparedExecute("INSERT INTO " + Const.U_TABLE + " (" + Const.U_ALL + ")" + "VALUES(?,?,?,?,?,?)",
                data.uuid, data.ip, escapeString(data.name), data.locale, data.admin, data.banned);
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
            res.next();
            return res.getString(1);
        }
    }

    public static String[] get(String uuid) throws SQLException {
        String select = "SELECT * FROM " + Const.U_TABLE + " WHERE " + Const.U_UUID + "=?";
        try (PreparedStatement prSt = getDbConnection().prepareStatement(select)) {
            prSt.setString(1, uuid);

            ResultSet data = prSt.executeQuery();
            data.next();
            String[] dataArray = new String[data.getMetaData().getColumnCount()];
            for (int i = 0; i < dataArray.length; i++) {
                dataArray[i] = data.getString(i);
            }
            return dataArray;
        }
    }

    public static void update(String uuid, String column, String value) throws SQLException {
        preparedExecute("UPDATE " + Const.U_TABLE + " SET " + column + "=? WHERE " + Const.U_UUID + "=?",
                escapeString(value), uuid);
    }
}
