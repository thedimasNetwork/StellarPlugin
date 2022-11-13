package stellar.database.entries;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import stellar.database.enums.ServerEventTypes;
import stellar.database.types.*;

import java.sql.Timestamp;

import static stellar.util.StringUtils.quote;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServerEventEntry extends Entry {
    String id;
    String server;
    int timestamp;
    ServerEventTypes type;
    String ip;
    String name;
    String uuid;
    String mapname;
    int wave;
    String request;

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", id, quote(server), timestamp, quote(type.toString()), quote(ip), quote(name), quote(uuid), quote(mapname), wave, quote(request));
    }

    public static ServerEventEntry fromString(String content) {
        return new ServerEventEntry(content);
    }

    public ServerEventEntry(String content) {
        String[] split = content.split(",");
        this.id = split[0];
        this.server = split[1];
        this.timestamp = Integer.parseInt(split[2]);
        this.type = ServerEventTypes.parse(split[3]);
        this.ip = split[4];
        this.name = split[5];
        this.uuid = split[6];
        this.mapname = split[7];
        this.wave = Integer.parseInt(split[8]);
        this.request = split[9];
    }

    public ServerEventEntry(String id, String server, int timestamp, ServerEventTypes type, String ip, String name, String uuid, String mapname, int wave, String request) {
        this.id = id;
        this.server = server;
        this.timestamp = timestamp;
        this.type = type;
        this.ip = ip;
        this.name = name;
        this.uuid = uuid;
        this.mapname = mapname;
        this.wave = wave;
        this.request = request;
    }
}
