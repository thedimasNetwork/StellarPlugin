package stellar.database.entries;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import stellar.database.tables.PlayerEvents;
import stellar.database.tables.Tables;
import stellar.database.tables.Users;
import stellar.database.types.Entry;
import stellar.database.types.Table;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayerEntry extends Entry {
    String uuid;
    String ip;
    String name;
    String locale;
    String translator;
    boolean admin;
    boolean jsallowed;
    int donated;
    boolean banned;
    int exp;

    public static PlayerEntryBuilder builder() {
        return new PlayerEntryBuilder()
                .translator("double");
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", uuid, ip, name, locale, translator, admin, jsallowed, donated, banned, exp);
    }

    public PlayerEntry(String uuid, String ip, String name, String locale, String translator, boolean admin, boolean jsallowed, int donated, boolean banned, int exp) {
        this.uuid = uuid;
        this.ip = ip;
        this.name = name;
        this.locale = locale;
        this.translator = translator;
        this.admin = admin;
        this.jsallowed = jsallowed;
        this.donated = donated;
        this.banned = banned;
        this.exp = exp;
    }
}
