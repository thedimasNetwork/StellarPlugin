package database;

public class Const {
    public static final String U_TABLE = "users";
    public static final String U_UUID = "uuid";
    public static final String U_ID = "id";
    public static final String U_NAME = "name";
    public static final String U_IS_ADMIN = "isAdmin";
    public static final String U_IS_BANNED = "isBanned";
    public static final String U_KICKS = "kicks";
    public static final String U_JOINS = "joins";
    public static final String U_LOCALE = "locale";
    public static final String U_ALL = String.format("%s,%s,%s,%s,%s,%s,%s,%s",
            U_UUID, U_ID, U_NAME, U_IS_ADMIN, U_IS_BANNED, U_KICKS, U_JOINS, U_LOCALE);
}
