package stellar.database.entries;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import stellar.database.enums.PlayerEventTypes;
import stellar.database.types.Entry;

import java.sql.Timestamp;

import static stellar.util.StringUtils.quote;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayerEventEntry extends Entry {
    String id;
    String server;
    int timestamp;
    PlayerEventTypes type;
    String uuid;
    String ip;
    String message;
    int x;
    int y;
    String block;
    String command;

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", id, quote(server), timestamp, quote(type.toString()), quote(uuid), quote(ip), quote(message), x, y, quote(block), quote(command));
    }

    public static PlayerEventEntry fromString(String content) {
        return new PlayerEventEntry(content);
    }

    public PlayerEventEntry(String content) {
        String[] split = content.split(",");
        this.id = split[0];
        this.server = split[1];
        this.timestamp = Integer.parseInt(split[2]);
        this.type = PlayerEventTypes.parse(split[3]);
        this.uuid = split[4];
        this.ip = split[5];
        this.message = split[6];
        this.x = Integer.parseInt(split[7]);
        this.y = Integer.parseInt(split[8]);
        this.block = split[9];
        this.command = split[10];
    }

    public PlayerEventEntry(String id, String server, int timestamp, PlayerEventTypes type, String uuid, String ip, String message, int x, int y, String block, String command) {
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
