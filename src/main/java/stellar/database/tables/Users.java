package stellar.database.tables;

import stellar.database.types.Field;
import stellar.database.types.Table;

public class Users extends Table {
    public final String title = "users";

    public final Field<String> uuid = new Field<>("uuid", String.class, title);
    public final Field<String> name = new Field<>("name", String.class, title);
    public final Field<String> ip = new Field<>("ip", String.class, title);
    public final Field<String> locale = new Field<>("locale", String.class, title);
    public final Field<String> translator = new Field<>("translator", String.class, title);
    public final Field<Boolean> admin = new Field<>("admin", Boolean.class, title);
    public final Field<Boolean> jsallowed = new Field<>("jsallowed", Boolean.class, title);
    public final Field<Integer> donated = new Field<>("donated", Integer.class, title);
    public final Field<Boolean> banned = new Field<>("banned", Boolean.class, title);
    public final Field<Integer> exp = new Field<>("exp", Integer.class, title);

    public final Field<String> key = uuid;

    public final String all = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", uuid, ip, name, locale, translator, admin, jsallowed, donated, banned, exp);
    public final String all_raw = all.replaceAll("([a-zA-Z_])+", "?"); // заменяет все символы кроме ',' на '?'

}
