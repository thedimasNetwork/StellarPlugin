package stellar.plugin.util;

import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import org.jooq.Field;
import stellar.database.gen.Tables;
import stellar.plugin.Variables;
import stellar.plugin.components.Rank;

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
                    statsMap.put(field.getName(), 0);
                }
            }
            Variables.statsData.put(player.uuid(), statsMap);
            Players.incrementStats(player, stat);
        }
    }

    public static String prefixName(Player player) {
        Rank rank = Variables.ranks.get(player.uuid(), Rank.player);
        Rank specialRank = Variables.specialRanks.get(player.uuid());

        if (rank.icon == null) {
            return player.admin() ? String.format("<%s> %s", specialRank.icon, player.coloredName()) : player.coloredName();
        } else {
            return String.format("<[#%s]%s[]> %s", rank.color, player.admin() ? specialRank.icon : rank.icon, player.coloredName());
        }
    }

    // I have no clue how this works...
    public static boolean fieldCompare(java.lang.reflect.Field field, Object object, String value, String comparator) {
        Log.debug(field.getType());
        try {
            if (UnlockableContent.class.isAssignableFrom(field.getType())) {
                Log.debug("A1");
                boolean result = false;
                if (comparator.endsWith("=")) {
                    result = ((UnlockableContent) field.get(object)).name.equals(value);
                } else if (comparator.endsWith("~")) {
                    String sn = StringUtils.stripColorsAndGlyphs(((UnlockableContent) field.get(object)).name);
                    String sv = StringUtils.stripColorsAndGlyphs(value);
                    result = sn.equalsIgnoreCase(sv);
                }
                return comparator.startsWith("!") != result;
            } else if (Number.class.isAssignableFrom(field.getType())) {
                Log.debug("B1");
                if (!Strings.canParseInt(value)) {
                    Log.debug("B2");
                    return false;
                }
                int fv = field.getInt(object);
                int v = Strings.parseInt(value);
                Log.debug("B3");
                switch (comparator) {
                    case ">=" -> {
                        return fv >= v;
                    }
                    case ">" -> {
                        return fv > v;
                    }
                    case "<=" -> {
                        return fv <= v;
                    }
                    case "<" -> {
                        return fv < v;
                    }
                    case "=", "~" -> {
                        return fv == v;
                    }
                    case "!=", "!~" -> {
                        return fv != v;
                    }
                }
            } else if (String.class.isAssignableFrom(field.getType())) {
                Log.debug("C1");
                boolean result = false;
                if (comparator.endsWith("=")) {
                    result = field.get(object).equals(value);
                } else if (comparator.endsWith("~")) {
                    String sn = StringUtils.stripColorsAndGlyphs((String) field.get(object));
                    String sv = StringUtils.stripColorsAndGlyphs(value);
                    result = sv.equalsIgnoreCase(sn);
                }
                return comparator.startsWith("!") != result;
            } else {
                boolean result = false;
                if (comparator.endsWith("=")) {
                    result = field.get(object).toString().equals(value);
                } else if (comparator.endsWith("~")) {
                    result = field.get(object).toString().equalsIgnoreCase(value);
                }
                return comparator.startsWith("!") != result;
            }
        } catch (IllegalAccessException e) {
            Log.debug("D1");
            return false;
        }
        Log.debug("E1");
        return false;
    }
}
