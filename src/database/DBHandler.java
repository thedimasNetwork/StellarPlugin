package database;

import arc.util.Log;

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

    public static void add(String[] data) throws SQLException {
        String insert = "INSERT INTO " + Const.U_TABLE + " (" + Const.U_ALL + ")" + "VALUES(?,?,?,?,?,?)";
        PreparedStatement prSt = getDbConnection().prepareStatement(insert);
        data[2] = data[2].replace("&", "&amp").replace("\"","&quot").replace("'","&apos");
        prSt.setString(1, data[0]); //uuid
        prSt.setString(2, data[1]); //ip
        //prSt.setString(3, data[2]); //id
        prSt.setString(3, data[2]); //name
        prSt.setString(4, data[3]); //locale
        prSt.setString(5, (data[4].equals("true") ? "1" : "0")); //admin
        prSt.setString(6, (data[5].equals("true") ? "1" : "0")); //banned

        prSt.executeUpdate();
    }

    public static boolean userExist(String uuid) throws SQLException {
        String select = "SELECT * FROM " + Const.U_TABLE + " WHERE " + Const.U_UUID + "=?";
        PreparedStatement prSt = getDbConnection().prepareStatement(select);
        prSt.setString(1, uuid);
        return prSt.executeQuery().next();
    }

    public static String get(String uuid, String column) throws SQLException {
        String select = "SELECT " + column + " FROM " + Const.U_TABLE + " WHERE " + Const.U_UUID + "=?";
        PreparedStatement prSt = getDbConnection().prepareStatement(select);
        prSt.setString(1, uuid);
        ResultSet res = prSt.executeQuery();
        res.next();
        return res.getString(1);
    }

    public static String[] get(String uuid) throws SQLException {
        String select = "SELECT * FROM " + Const.U_TABLE + " WHERE " + Const.U_UUID + "=?";
        PreparedStatement prSt = getDbConnection().prepareStatement(select);
        prSt.setString(1, uuid);
        ResultSet data = prSt.executeQuery();
        data.next();
        String[] dataArray = new String[data.getMetaData().getColumnCount()];
        for (int i = 0; i < dataArray.length; i++) {
            dataArray[i] = data.getString(i);
        }
        return dataArray;
    }

    public static void update(String uuid, String column, String value) throws SQLException {
        value = value.replace("&", "&amp").replace("\"","&quot").replace("'","&apos");
        String update = "UPDATE " + Const.U_TABLE + " SET " + column + "=? WHERE " + Const.U_UUID + "=?";
        PreparedStatement prSt = getDbConnection().prepareStatement(update);
        prSt.setString(1, value);
        prSt.setString(2, uuid);
        prSt.executeUpdate();
    }
}
