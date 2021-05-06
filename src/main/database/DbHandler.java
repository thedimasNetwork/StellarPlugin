package main.database;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.LinkedList;

public class DbHandler {

    public Connection getDbConnection() throws SQLException {
        String connectionURL = "jdbc:mariadb://" + Config.DB_HOST + ":" + Config.DB_PORT + "/" + Config.DB_NAME;
        return DriverManager.getConnection(connectionURL, Config.DB_USER, Config.DB_PASS);
    }

    public void addUser(String[] data) throws SQLException {
        String insert = "INSERT INTO " + Const.U_TABLE + " (" + Const.ALL + ")" + "VALUES(?,?)";
        PreparedStatement prSt = getDbConnection().prepareStatement(insert);
        prSt.setString(1, data[0]);
        prSt.setString(2, data[1]);
        prSt.setString(3, data[2]);
        prSt.setString(4, data[3]);
        prSt.setString(5, data[4]);
        prSt.setString(6, data[5]);
        prSt.setString(7, data[6]);
        prSt.setString(8, data[7]);
        prSt.executeUpdate();
    }

    public boolean userExist(String uuid) throws SQLException {
        String select = "SELECT * FROM " + Const.U_UUID + " WHERE " + Const.U_UUID + "=?";
        PreparedStatement prSt = getDbConnection().prepareStatement(select);
        prSt.setString(1, uuid);
        return prSt.executeQuery().next();
    }

    public String get(String uuid, String column) throws SQLException {
        String select = "SELECT " + column + " FROM " + Const.U_UUID + " WHERE " + Const.U_UUID + "=?";
        PreparedStatement prSt = getDbConnection().prepareStatement(select);
        prSt.setString(1, uuid);
        return prSt.executeQuery().getString(1);
    }

    public String[] getAll(String uuid) throws SQLException {
        String select = "SELECT * FROM " + Const.U_UUID + " WHERE " + Const.U_UUID + "=?";
        PreparedStatement prSt = getDbConnection().prepareStatement(select);
        prSt.setString(1, uuid);
        ResultSet data = prSt.executeQuery();
        LinkedList<String> dataList = new LinkedList<>();
        for (int i = 0; data.next(); i++) {
            dataList.add(data.getString(i));
        }
        return (String[]) dataList.toArray();
    }

    public String getHashCode(String password) throws NoSuchAlgorithmException {
        StringBuilder encryptedPassword = new StringBuilder();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] passwordBytes = md5.digest(password.getBytes());
        for (byte b : passwordBytes) {
            encryptedPassword.append(String.format("%02X", b));
        }
        return encryptedPassword.toString().toLowerCase();
    }
}