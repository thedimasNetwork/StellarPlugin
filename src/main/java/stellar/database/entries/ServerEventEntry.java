package stellar.database.entries;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import stellar.database.tables.ServerEvents;
import stellar.database.tables.Tables;
import stellar.database.types.*;

import java.sql.Timestamp;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServerEventEntry extends Entry {
    String id;
    String server;
    Timestamp timestamp;
    String type;
    String ip;
    String name;
    String uuid;
    String mapname;
    int wave;

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s", id, server, timestamp, type, ip, name, uuid, mapname, wave);
    }

    public static ServerEventEntry fromString(String content) {
        return new ServerEventEntry(content);
    }

    public ServerEventEntry(String content) {
        String[] split = content.split(",");
        this.id = split[0];
        this.server = split[1];
        this.timestamp = Timestamp.valueOf(split[2]);
        this.type = split[3];
        this.ip = split[4];
        this.name = split[5];
        this.uuid = split[6];
        this.mapname = split[7];
        this.wave = Integer.parseInt(split[8]);
    }

    public ServerEventEntry(String id, String server, Timestamp timestamp, String type, String ip, String name, String uuid, String mapname, int wave) {
        this.id = id;
        this.server = server;
        this.timestamp = timestamp;
        this.type = type;
        this.ip = ip;
        this.name = name;
        this.uuid = uuid;
        this.mapname = mapname;
        this.wave = wave;
    }
}
