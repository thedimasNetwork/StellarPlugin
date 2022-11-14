package stellar.database.entries;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import stellar.database.types.Entry;

import static stellar.util.StringUtils.quote;

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

    public PlayerEntry(String content) {
        String[] split = content.split(",");
        this.uuid = split[0];
        this.ip = split[1];
        this.name = split[2];
        this.locale = split[3];
        this.translator = split[4];
        this.admin = Boolean.parseBoolean(split[5]);
        this.jsallowed = Boolean.parseBoolean(split[6]);
        this.donated = Integer.parseInt(split[7]);
        this.banned = Boolean.parseBoolean(split[8]);
        this.exp = Integer.parseInt(split[9]);
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

    public static PlayerEntryBuilder builder() {
        return new PlayerEntryBuilder()
                .translator("double");
    }

    public static PlayerEntry fromString(String content) {
        return new PlayerEntry(content);
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", quote(uuid), quote(ip), quote(name), quote(locale), quote(translator), admin, jsallowed, donated, banned, exp);
    }
}
