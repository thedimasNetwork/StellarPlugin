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
    int id;
    String server;
    Timestamp timestamp;
    String type;
    String ip;
    String name;
    String uuid;
    String mapname;
    String wave;

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s", id, server, timestamp, type, ip, name, uuid, mapname, wave);
    }

    public ServerEventEntry(int id, String server, Timestamp timestamp, String type, String ip, String name, String uuid, String mapname, String wave) {
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
