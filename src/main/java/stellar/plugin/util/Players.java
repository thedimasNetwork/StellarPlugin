package stellar.plugin.util;

import arc.util.Nullable;
import arc.util.Strings;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import stellar.plugin.Variables;

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
}
