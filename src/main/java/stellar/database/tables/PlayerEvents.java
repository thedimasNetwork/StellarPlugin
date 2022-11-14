package stellar.database.tables;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import stellar.database.types.Field;
import stellar.database.types.Table;

import java.sql.Timestamp;
import java.util.Map;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerEvents extends Table {
    Field<String> id = new Field<>("id", String.class, title);
    Field<String> server = new Field<>("server", String.class, title);
    Field<Integer> timestamp = new Field<>("timestamp", Integer.class, title);
    Field<String> type = new Field<>("type", String.class, title);
    Field<String> uuid = new Field<>("uuid", String.class, title);
    Field<String> ip = new Field<>("ip", String.class, title);
    Field<String> name = new Field<>("name", String.class, title);
    Field<String> message = new Field<>("message", String.class, title);
    Field<Integer> x = new Field<>("x", Integer.class, title);
    Field<Integer> y = new Field<>("y", Integer.class, title);
    Field<String> block = new Field<>("block", String.class, title);
    Field<String> command = new Field<>("command", String.class, title);

    public PlayerEvents() {
        this.title = "player_events";
        this.key = id;
        this.all = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", id, server, timestamp, type, uuid, ip, name, message, x, y, block, command);
        this.allRaw = all.replaceAll("([a-zA-Z_])+", "?"); // заменяет все символы кроме ',' на '?'
    }

}
