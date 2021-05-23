package database;

public class Table {
    public static final String U_TABLE = "users";
    public static final String U_UUID = "uuid";
    public static final String U_NAME = "name";
    public static final String U_IP = "ip";
    public static final String U_LOCALE = "locale";
    public static final String U_TRANSLATOR = "translator";
    public static final String U_PLAY_TIME = "play_time";
    public static final String U_ADMIN = "admin";
    public static final String U_BANNED = "banned";
    public static final String U_ALL = String.format("%s,%s,%s,%s,%s,%s,%s,%s", U_UUID, U_IP, U_NAME, U_LOCALE, U_TRANSLATOR, U_PLAY_TIME, U_ADMIN, U_BANNED);
    public static final String U_ALL_RAW = U_ALL.replaceAll("([a-zA-Z_])+", "?"); // заменяет все символы кроме ',' на '?'

    public static final Row<String> UUID = new Row<>(U_UUID, String.class);
    public static final Row<String> NAME = new Row<>(U_NAME, String.class);
    public static final Row<String> IP = new Row<>(U_IP, String.class);
    public static final Row<String> LOCALE = new Row<>(U_LOCALE, String.class);
    public static final Row<String> TRANSLATOR = new Row<>(U_TRANSLATOR, String.class);
    public static final Row<Long> PLAY_TIME = new Row<>(U_PLAY_TIME, Long.class);
    public static final Row<Boolean> ADMIN = new Row<>(U_ADMIN, Boolean.class);
    public static final Row<Boolean> BANNED = new Row<>(U_BANNED, Boolean.class);
}
