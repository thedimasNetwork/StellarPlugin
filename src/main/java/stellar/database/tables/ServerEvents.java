package stellar.database.tables;

import stellar.database.types.Field;
import stellar.database.types.Table;

import java.sql.Timestamp;

public class ServerEvents extends Table {
    public final String title = "server_events";

    public final Field<Integer> id = new Field<>("id", Integer.class, title);
    public final Field<String> server = new Field<>("server", String.class, title);
    public final Field<Timestamp> timestamp = new Field<>("timestamp", Timestamp.class, title);
    public final Field<String> type = new Field<>("type", String.class, title);
    public final Field<String> ip = new Field<>("ip", String.class, title);
    public final Field<String> name = new Field<>("name", String.class, title);
    public final Field<String> uuid = new Field<>("uuid", String.class, title);
    public final Field<String> mapname = new Field<>("mapname", String.class, title);
    public final Field<Integer> wave = new Field<>("wave", Integer.class, title);

    public final Field<Integer> key = id;

    public final String all = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s", id, server, timestamp, type, ip, name, uuid, mapname, wave);
    public final String all_raw = all.replaceAll("([a-zA-Z_])+", "?"); // заменяет все символы кроме ',' на '?'

}
