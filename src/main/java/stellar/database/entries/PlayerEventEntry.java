package stellar.database.entries;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import stellar.database.tables.PlayerEvents;
import stellar.database.tables.Tables;
import stellar.database.types.Entry;
import stellar.database.types.Table;

import java.sql.Timestamp;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayerEventEntry extends Entry {
    int id;
    String server;
    Timestamp timestamp;
    String type;
    String uuid;
    String ip;
    String message;
    int x;
    int y;
    String block;
    String command;

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", id, server, timestamp, type, uuid, ip, message, x, y, block, command);
    }

    public PlayerEventEntry(int id, String server, Timestamp timestamp, String type, String uuid, String ip, String message, int x, int y, String block, String command) {
        this.id = id;
        this.server = server;
        this.timestamp = timestamp;
        this.type = type;
        this.uuid = uuid;
        this.ip = ip;
        this.message = message;
        this.x = x;
        this.y = y;
        this.block = block;
        this.command = command;
    }
}
