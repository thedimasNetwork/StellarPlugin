package stellar.database.entries;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import stellar.database.tables.Tables;
import stellar.database.types.*;

import java.sql.Timestamp;

import static stellar.util.StringUtils.quote;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlaytimeEntry extends Entry {
    String uuid;
    int hub;
    int survival;
    int attack;
    int sandbox;
    int pvp;
    int annexation;
    int anarchy;
    int campaign_maps;
    int ms_go;
    int hex_pvp;
    int castle_wars;
    int crawler_arena;
    int zone_capture;

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", quote(uuid), hub, survival, attack, sandbox, pvp, annexation, anarchy, campaign_maps, ms_go, hex_pvp, castle_wars, crawler_arena, zone_capture);
    }

    public static PlaytimeEntry fromString(String content) {
        return new PlaytimeEntry(content);
    }

    public PlaytimeEntry(String content) {
        String[] split = content.split(",");
        this.uuid = split[0];
        this.hub = Integer.parseInt(split[1]);
        this.survival = Integer.parseInt(split[2]);
        this.attack = Integer.parseInt(split[3]);
        this.sandbox = Integer.parseInt(split[4]);
        this.pvp = Integer.parseInt(split[5]);
        this.annexation = Integer.parseInt(split[6]);
        this.anarchy = Integer.parseInt(split[7]);
        this.campaign_maps = Integer.parseInt(split[8]);
        this.ms_go = Integer.parseInt(split[9]);
        this.hex_pvp = Integer.parseInt(split[10]);
        this.castle_wars = Integer.parseInt(split[11]);
        this.crawler_arena = Integer.parseInt(split[12]);
        this.zone_capture = Integer.parseInt(split[13]);
    }

    public PlaytimeEntry(String uuid, int hub, int survival, int attack, int sandbox, int pvp, int annexation, int anarchy, int campaign_maps, int ms_go, int hex_pvp, int castle_wars, int crawler_arena, int zone_capture) {
        this.uuid = uuid;
        this.hub = hub;
        this.survival = survival;
        this.attack = attack;
        this.sandbox = sandbox;
        this.pvp = pvp;
        this.annexation = annexation;
        this.anarchy = anarchy;
        this.campaign_maps = campaign_maps;
        this.ms_go = ms_go;
        this.hex_pvp = hex_pvp;
        this.castle_wars = castle_wars;
        this.crawler_arena = crawler_arena;
        this.zone_capture = zone_capture;
    }
}
