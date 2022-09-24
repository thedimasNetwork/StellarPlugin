package stellar.database;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayerData {

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

    public static PlayerDataBuilder builder() {
        return new PlayerDataBuilder()
                .translator("double");
    }

}
