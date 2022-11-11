package stellar.database.tables;

import stellar.database.types.*;
import java.util.*;

public class Playtime extends Table {
    public final String title = "playtime";

    public final Field<String> uuid = new Field<>("uuid", String.class, title);
    public final Field<Long> hub = new Field<>("hub", Long.class, title);
    public final Field<Long> survival = new Field<>("survival", Long.class, title);
    public final Field<Long> attack = new Field<>("attack", Long.class, title);
    public final Field<Long> sandbox = new Field<>("sandbox", Long.class, title);
    public final Field<Long> pvp = new Field<>("pvp", Long.class, title);
    public final Field<Long> annexation = new Field<>("annexation", Long.class, title);
    public final Field<Long> anarchy = new Field<>("anarchy", Long.class, title);
    public final Field<Long> campaign_maps = new Field<>("campaign_maps", Long.class, title);
    public final Field<Long> ms_go = new Field<>("ms_go", Long.class, title);
    public final Field<Long> hex_pvp = new Field<>("hex_pvp", Long.class, title);
    public final Field<Long> castle_wars = new Field<>("castle_wars", Long.class, title);
    public final Field<Long> crawler_arena = new Field<>("crawler_arena", Long.class, title);
    public final Field<Long> zone_capture = new Field<>("zone_capture", Long.class, title);

    public final Field<String> key = uuid;

    public final String all = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", uuid, hub, survival, attack, sandbox, pvp, annexation, anarchy, campaign_maps, ms_go, hex_pvp, castle_wars, crawler_arena, zone_capture);
    public final String all_raw = all.replaceAll("([a-zA-Z_])+", "?"); // заменяет все символы кроме ',' на '?'
    public final Map<String, Field<Long>> fields = new HashMap<>();

    public Playtime() {
        fields.put("hub", hub);
        fields.put("survival", survival);
        fields.put("attack", attack);
        fields.put("sandbox", sandbox);
        fields.put("pvp", pvp);
        fields.put("annexation", annexation);
        fields.put("anarchy", anarchy);
        fields.put("campaign_maps", campaign_maps);
        fields.put("ms_go", ms_go);
        fields.put("hex_pvp", hex_pvp);
        fields.put("castle_wars", castle_wars);
        fields.put("crawler_arena", crawler_arena);
        fields.put("zone_capture", zone_capture);
    }
}
