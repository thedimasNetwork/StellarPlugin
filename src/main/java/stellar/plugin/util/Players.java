package stellar.plugin.util;

import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import org.jooq.Field;
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
        String replacedName = name.replace('_', ' ').strip();
        return Groups.player.find(p -> name.equalsIgnoreCase(Strings.stripColors(p.name).strip())
                || name.equalsIgnoreCase(StringUtils.stripColorsAndGlyphs(p.name).strip())
                || replacedName.equalsIgnoreCase(Strings.stripColors(p.name).strip())
                || replacedName.equalsIgnoreCase(StringUtils.stripColorsAndGlyphs(p.name).strip()));
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
            ObjectMap<String, Integer> statsMap = new ObjectMap<>();
            for (Field<?> field : Tables.stats.fields()) {
                if (field.getType() == Integer.class || field.getType() == Long.class) {
                    Tables.playtime.erekirHexed.getUnqualifiedName();
                    statsMap.put(field.getName(), 0);
                }
            }
            Variables.statsData.put(player.uuid(), statsMap);
            Players.incrementStats(player, stat);
        }
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
