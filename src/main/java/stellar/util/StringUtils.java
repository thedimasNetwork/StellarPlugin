package stellar.util;

import arc.util.Strings;

public class StringUtils {
    public static String stripColorsAndGlyphs(String str) {
        str = Strings.stripColors(str);
        return Strings.stripGlyphs(str);
    }

    public static String quote(String in) {
        return in != null ? ("'" + in + "'") : null;
    }
}
