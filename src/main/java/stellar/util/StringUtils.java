package stellar.util;

import arc.util.Strings;

public class StringUtils {
    public static String stripColorsAndGlyphs(String str) {
        str = Strings.stripColors(str);
        return Strings.stripGlyphs(str);
    }
}
