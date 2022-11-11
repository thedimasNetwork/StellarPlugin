package stellar.database.tables;

import stellar.database.types.Field;
import stellar.database.types.Table;

import java.sql.Timestamp;

public class PlayerEvents extends Table {
    public final String title = "server_events";

    public final Field<Integer> id = new Field<>("id", Integer.class, title);
    public final Field<String> server = new Field<>("server", String.class, title);
    public final Field<Timestamp> timestamp = new Field<>("timestamp", Timestamp.class, title);
    public final Field<String> type = new Field<>("type", String.class, title);
    public final Field<String> uuid = new Field<>("uuid", String.class, title);
    public final Field<String> ip = new Field<>("ip", String.class, title);
    public final Field<String> message = new Field<>("message", String.class, title);
    public final Field<Integer> x = new Field<>("x", Integer.class, title);
    public final Field<Integer> y = new Field<>("y", Integer.class, title);
    public final Field<String> block = new Field<>("block", String.class, title);
    public final Field<String> command = new Field<>("command", String.class, title);

    public final Field<Integer> key = id;

    public final String all = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", id, server, timestamp, type, uuid, ip, message, x, y, block, command);
    public final String all_raw = all.replaceAll("([a-zA-Z_])+", "?"); // заменяет все символы кроме ',' на '?'


}
