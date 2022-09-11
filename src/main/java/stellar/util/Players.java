package stellar.util;

import arc.util.Strings;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class Players {
    public static Player findPlayer(String name) {
        String replacedName = name.replace('_', ' ');
        return Groups.player.find(p -> name.equals(Strings.stripColors(p.name))
                || name.equals(StringUtils.stripColorsAndGlyphs(p.name))
                || replacedName.equals(Strings.stripColors(p.name))
                || replacedName.equals(StringUtils.stripColorsAndGlyphs(p.name)));
    }
}
