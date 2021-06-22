package stellar.database.tables;

import stellar.database.Field;

public class Users {
    public static final String U_TABLE = "users";
    public static final String U_UUID = "uuid";
    public static final String U_NAME = "name";
    public static final String U_IP = "ip";
    public static final String U_LOCALE = "locale";
    public static final String U_TRANSLATOR = "translator";
    public static final String U_ADMIN = "admin";
    public static final String U_BANNED = "banned";

    public static final String U_ALL = String.format("%s,%s,%s,%s,%s,%s,%s", U_UUID, U_IP, U_NAME, U_LOCALE, U_TRANSLATOR, U_ADMIN, U_BANNED);
    public static final String U_ALL_RAW = U_ALL.replaceAll("([a-zA-Z_])+", "?"); // заменяет все символы кроме ',' на '?'

    public static final Field<String> UUID = new Field<>(U_UUID, String.class, U_TABLE);
    public static final Field<String> NAME = new Field<>(U_NAME, String.class, U_TABLE);
    public static final Field<String> IP = new Field<>(U_IP, String.class, U_TABLE);
    public static final Field<String> LOCALE = new Field<>(U_LOCALE, String.class, U_TABLE);
    public static final Field<String> TRANSLATOR = new Field<>(U_TRANSLATOR, String.class, U_TABLE);
    public static final Field<Boolean> ADMIN = new Field<>(U_ADMIN, Boolean.class, U_TABLE);
    public static final Field<Boolean> BANNED = new Field<>(U_BANNED, Boolean.class, U_TABLE);
}
