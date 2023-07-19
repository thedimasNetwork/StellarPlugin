package stellar.plugin.util;

import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import org.jooq.Record1;
import stellar.database.Database;
import stellar.database.gen.Tables;
import stellar.plugin.Variables;
import stellar.plugin.components.Rank;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Players {
   @Nullable
   public static Player getPlayer(String uuid) {
       return Groups.player.find(player -> player.uuid().equals(uuid));
   }

    @Nullable
    public static Player findPlayer(String name) {
        String replacedName = name.replace('_', ' ');
        return Groups.player.find(p -> name.equals(Strings.stripColors(p.name))
                || name.equals(StringUtils.stripColorsAndGlyphs(p.name))
                || replacedName.equals(Strings.stripColors(p.name))
                || replacedName.equals(StringUtils.stripColorsAndGlyphs(p.name)));
    }

    public static boolean isBot(Player player) {
        AtomicBoolean blocked = new AtomicBoolean(false);
        Variables.blacklistedSubnets.each(subnet -> {
            if (NetUtils.isIPInSubnet(player.ip(), subnet)) {
                blocked.set(true);
            }
        });
        return blocked.get();
    }

    public static void incrementStats(Player player, String stat) {
        if (Variables.statsData.containsKey(player.uuid())) {
            int value = Variables.statsData.get(player.uuid()).get(stat);
            Variables.statsData.get(player.uuid()).put(stat, value + 1);  
        } else {
            Variables.statsData.put(player.uuid(), ObjectMap.of(
                    "attacks", 0,
                    "waves", 0,
                    "built", 0,
                    "broken", 0,
                    "deaths", 0,
                    "logins", 0,
                    "messages", 0
            ));
            Players.incrementStats(player, stat);
        }
    }

    public static long totalPlaytime(String uuid) throws SQLException { // FIXME: move into DB library
        Record1<Long> result = Database.getContext().select(
                Tables.PLAYTIME.HUB
                        .plus(Tables.PLAYTIME.SURVIVAL)
                        .plus(Tables.PLAYTIME.SANDBOX)
                        .plus(Tables.PLAYTIME.ATTACK)
                        .plus(Tables.PLAYTIME.PVP)
                        .plus(Tables.PLAYTIME.CAMPAIGN_MAPS)
                        .plus(Tables.PLAYTIME.MS_GO)
                        .plus(Tables.PLAYTIME.HEX_PVP)
                        .plus(Tables.PLAYTIME.EREKIR_HEXED)
                        .plus(Tables.PLAYTIME.CASTLE_WARS)
                        .plus(Tables.PLAYTIME.CRAWLER_ARENA)
                        .plus(Tables.PLAYTIME.ZONE_CAPTURE))
                .from(Tables.PLAYTIME)
                .where(Tables.PLAYTIME.UUID.eq(uuid))
                .fetchOne();
        return result == null ? 0 : result.value1();
    }

    public static String prefixName(Player player) {
       try {
           Rank rank = Rank.getRank(player);
           if (rank.icon == null) {
               return player.admin() ? "<\uE82C> " + player.coloredName() : player.coloredName();
           } else {
               return player.admin() ? String.format("<[#%s]\uE82C[]> %s", rank.color, player.coloredName()) :
                       String.format("<[#%s]%s[]> %s", rank.color, rank.icon, player.coloredName());
           }
       } catch (SQLException e) {
           Log.err(e);
           return player.admin() ? "<\uE872> " + player.coloredName() : player.coloredName();
       }
    }
}
