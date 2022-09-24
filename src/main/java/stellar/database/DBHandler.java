package stellar.database;

import arc.util.Log;
import arc.util.Nullable;

import stellar.database.tables.Playtime;
import stellar.database.tables.Users;

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

    public static void save(PlayerData data) throws SQLException {
        preparedExecute("INSERT INTO " + Users.U_TABLE + " (" + Users.U_ALL + ") VALUES (" + Users.U_ALL_RAW + ")",
                data.getUuid(),
                data.getIp(),
                escapeString(data.getName()),
                data.getLocale(),
                data.getTranslator(),
                data.isAdmin(),
                data.isJsallowed(),
                data.getDonated(),
                data.isBanned(),
                data.getExp());

        preparedExecute("INSERT INTO " + Playtime.P_TABLE + " (" + Playtime.P_UUID + ") VALUES (?)", data.getUuid());
    }

    private static String escapeString(String text) {
        return text.replace("&", "&amp").replace("\"", "&quot").replace("'", "&apos");
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

            return PlayerData.builder()
                    .uuid(data.getString(Users.U_UUID))
                    .ip(data.getString(Users.U_IP))
                    .name(data.getString(Users.U_NAME))
                    .locale(data.getString(Users.U_LOCALE))
                    .translator(data.getString(Users.U_TRANSLATOR))
                    .admin(data.getBoolean(Users.U_ADMIN))
                    .jsallowed(data.getBoolean(Users.U_JSALLOWED))
                    .donated(data.getInt(Users.U_DONATED))
                    .banned(data.getBoolean(Users.U_BANNED))
                    .exp(data.getInt(Users.U_EXP))
                    .build();
        }
    }

    public static Set<PlayerData> getByIp(String ip) throws SQLException {
        Set<PlayerData> result = new HashSet<>();
        String select = "SELECT " + Users.U_ALL + " FROM " + Users.U_TABLE + " WHERE " + Users.U_UUID + "=?";
        try (PreparedStatement prSt = getDbConnection().prepareStatement(select)) {
            prSt.setString(1, ip);

            ResultSet data = prSt.executeQuery();
            while (data.next()) {
                result.add(PlayerData.builder()
                        .uuid(data.getString(Users.U_UUID))
                        .ip(data.getString(Users.U_IP))
                        .name(data.getString(Users.U_NAME))
                        .locale(data.getString(Users.U_LOCALE))
                        .translator(data.getString(Users.U_TRANSLATOR))
                        .admin(data.getBoolean(Users.U_ADMIN))
                        .jsallowed(data.getBoolean(Users.U_JSALLOWED))
                        .donated(data.getInt(Users.U_DONATED))
                        .banned(data.getBoolean(Users.U_BANNED))
                        .exp(data.getInt(Users.U_EXP))
                        .build());
            }
            return result;
        }
    }

    public static <T> void update(String uuid, Field<T> column, T value) throws SQLException {
        preparedExecute("UPDATE " + column.getTable() + " SET " + column.getName() + "=? WHERE " + Users.U_UUID + "=?",
                value instanceof Boolean ? value : escapeString(String.valueOf(value)), uuid);
    }

}
