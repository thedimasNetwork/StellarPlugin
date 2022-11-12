package stellar.database.tables;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import stellar.database.types.Field;
import stellar.database.types.Table;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Users extends Table {
    Field<String> uuid = new Field<>("uuid", String.class, title);
    Field<String> name = new Field<>("name", String.class, title);
    Field<String> ip = new Field<>("ip", String.class, title);
    Field<String> locale = new Field<>("locale", String.class, title);
    Field<String> translator = new Field<>("translator", String.class, title);
    Field<Boolean> admin = new Field<>("admin", Boolean.class, title);
    Field<Boolean> jsallowed = new Field<>("jsallowed", Boolean.class, title);
    Field<Integer> donated = new Field<>("donated", Integer.class, title);
    Field<Boolean> banned = new Field<>("banned", Boolean.class, title);
    Field<Integer> exp = new Field<>("exp", Integer.class, title);

    public Users() {
        this.title = "users";
        this.key = uuid;
        this.all = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", uuid, ip, name, locale, translator, admin, jsallowed, donated, banned, exp);
        this.allRaw = all.replaceAll("([a-zA-Z_])+", "?"); // заменяет все символы кроме ',' на '?'
    }
}
