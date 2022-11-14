package stellar.database.tables;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import stellar.database.types.Field;
import stellar.database.types.Table;

import java.util.HashMap;
import java.util.Map;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Playtime extends Table {
    Field<String> uuid = new Field<>("uuid", String.class, title);
    Field<Long> hub = new Field<>("hub", Long.class, title);
    Field<Long> survival = new Field<>("survival", Long.class, title);
    Field<Long> attack = new Field<>("attack", Long.class, title);
    Field<Long> sandbox = new Field<>("sandbox", Long.class, title);
    Field<Long> pvp = new Field<>("pvp", Long.class, title);
    Field<Long> annexation = new Field<>("annexation", Long.class, title);
    Field<Long> anarchy = new Field<>("anarchy", Long.class, title);
    Field<Long> campaignMaps = new Field<>("campaign_maps", Long.class, title);
    Field<Long> msgo = new Field<>("ms_go", Long.class, title);
    Field<Long> hexPvp = new Field<>("hex_pvp", Long.class, title);
    Field<Long> castleWars = new Field<>("castle_wars", Long.class, title);
    Field<Long> crawlerArena = new Field<>("crawler_arena", Long.class, title);
    Field<Long> zoneCapture = new Field<>("zone_capture", Long.class, title);

    Map<String, Field<Long>> fields = new HashMap<>();

    public Playtime() {
        fields.put("hub", hub);
        fields.put("survival", survival);
        fields.put("attack", attack);
        fields.put("sandbox", sandbox);
        fields.put("pvp", pvp);
        fields.put("annexation", annexation);
        fields.put("anarchy", anarchy);
        fields.put("campaign_maps", campaignMaps);
        fields.put("ms_go", msgo);
        fields.put("hex_pvp", hexPvp);
        fields.put("castle_wars", castleWars);
        fields.put("crawler_arena", crawlerArena);
        fields.put("zone_capture", zoneCapture);
        this.title = "playtime";
        this.key = uuid;
        this.all = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", uuid, hub, survival, attack, sandbox, pvp, annexation, anarchy, campaignMaps,  msgo, hexPvp, castleWars, crawlerArena, zoneCapture);
        this.allRaw = all.replaceAll("([a-zA-Z_])+", "?"); // заменяет все символы кроме ',' на '?'
    }
}
