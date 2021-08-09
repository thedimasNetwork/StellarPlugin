package stellar.database.tables;

import stellar.database.Field;

import java.util.*;

public class Playtime {
    public static final String P_TABLE = "playtime";
    public static final String P_UUID = "uuid";
    public static final String P_HUB = "hub";
    public static final String P_SURVIVAL = "survival";
    public static final String P_ATTACK = "attack";
    public static final String P_SANDBOX = "sandbox";
    public static final String P_PVP = "pvp";
    public static final String P_ANNEXATION = "annexation";
    public static final String P_ANARCHY = "anarchy";
    public static final String P_CAMPAIGN_MAPS = "campaign_maps";
    public static final String P_MS_GO = "ms_go";
    public static final String P_HEX_PVP = "hex_pvp";
    public static final String P_CASTLE_WARS = "castle_wars";
    public static final String P_CRAWLER_ARENA = "crawler_arena";
    public static final String P_ZONE_CAPTURE = "zone_capture";

    public static final Field<String> UUID = new Field<>(P_UUID, String.class, P_TABLE);
    public static final Field<Long> HUB = new Field<>(P_HUB, Long.class, P_TABLE);
    public static final Field<Long> SURVIVAL = new Field<>(P_SURVIVAL, Long.class, P_TABLE);
    public static final Field<Long> ATTACK = new Field<>(P_ATTACK, Long.class, P_TABLE);
    public static final Field<Long> SANDBOX = new Field<>(P_SANDBOX, Long.class, P_TABLE);
    public static final Field<Long> PVP = new Field<>(P_PVP, Long.class, P_TABLE);
    public static final Field<Long> ANNEXATION = new Field<>(P_ANNEXATION, Long.class, P_TABLE);
    public static final Field<Long> ANARCHY = new Field<>(P_ANARCHY, Long.class, P_TABLE);
    public static final Field<Long> CAMPAIGN_MAPS = new Field<>(P_CAMPAIGN_MAPS, Long.class, P_TABLE);
    public static final Field<Long> MS_GO = new Field<>(P_MS_GO, Long.class, P_TABLE);
    public static final Field<Long> HEX_PVP = new Field<>(P_HEX_PVP, Long.class, P_TABLE);
    public static final Field<Long> CASTLE_WARS = new Field<>(P_CASTLE_WARS, Long.class, P_TABLE);
    public static final Field<Long> CRAWLER_ARENA = new Field<>(P_CRAWLER_ARENA, Long.class, P_TABLE);
    public static final Field<Long> ZONE_CAPTURE = new Field<>(P_ZONE_CAPTURE, Long.class, P_TABLE);

    public static final Map<String, Field<Long>> FIELDS = new HashMap<>();

    static {
        FIELDS.put(P_HUB, HUB);
        FIELDS.put(P_SURVIVAL, SURVIVAL);
        FIELDS.put(P_ATTACK, ATTACK);
        FIELDS.put(P_SANDBOX, SANDBOX);
        FIELDS.put(P_PVP, PVP);
        FIELDS.put(P_ANNEXATION, ANNEXATION);
        FIELDS.put(P_ANARCHY, ANARCHY);
        FIELDS.put(P_CAMPAIGN_MAPS, CAMPAIGN_MAPS);
        FIELDS.put(P_MS_GO, MS_GO);
        FIELDS.put(P_HEX_PVP, HEX_PVP);
        FIELDS.put(P_CASTLE_WARS, CASTLE_WARS);
        FIELDS.put(P_CRAWLER_ARENA, CRAWLER_ARENA);
        FIELDS.put(P_ZONE_CAPTURE, ZONE_CAPTURE);
    }
}
