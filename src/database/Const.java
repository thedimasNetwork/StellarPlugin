package database;

public class Const {
    public static final String U_TABLE = "users";
    public static final String U_UUID = "uuid";
    // public static final String U_ID = "id";
    public static final String U_NAME = "name";
    public static final String U_ADMIN = "admin";
    public static final String U_LOCALE = "locale";
    public static final String U_IP = "ip";
    public static final String U_BANNED = "banned";
    public static final String U_ALL = String.format("%s,%s,%s,%s,%s,%s", U_UUID, U_IP, U_NAME, U_LOCALE, U_ADMIN, U_BANNED);
}
