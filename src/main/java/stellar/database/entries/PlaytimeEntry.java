package stellar.database.entries;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import stellar.database.tables.Tables;
import stellar.database.types.*;

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
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", uuid, hub, survival, attack, sandbox, pvp, annexation, anarchy, campaign_maps, ms_go, hex_pvp, castle_wars, crawler_arena, zone_capture);
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
