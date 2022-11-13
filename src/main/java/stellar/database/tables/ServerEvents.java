package stellar.database.tables;

import stellar.database.types.Field;
import stellar.database.types.Table;

public class ServerEvents extends Table {
    public final Field<String> id = new Field<>("id", String.class, title);
    public final Field<String> server = new Field<>("server", String.class, title);
    public final Field<Integer> timestamp = new Field<>("timestamp", Integer.class, title);
    public final Field<String> type = new Field<>("type", String.class, title);
    public final Field<String> ip = new Field<>("ip", String.class, title);
    public final Field<String> name = new Field<>("name", String.class, title);
    public final Field<String> uuid = new Field<>("uuid", String.class, title);
    public final Field<String> mapname = new Field<>("mapname", String.class, title);
    public final Field<Integer> wave = new Field<>("wave", Integer.class, title);
    public final Field<String> request = new Field<>("request", String.class, title);

    public ServerEvents() {
        this.title = "server_events";
        this.key = id;
        this.all = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", id, server, timestamp, type, ip, name, uuid, mapname, wave, request);
        this.allRaw = all.replaceAll("([a-zA-Z_])+", "?"); // заменяет все символы кроме ',' на '?'
    }
}
