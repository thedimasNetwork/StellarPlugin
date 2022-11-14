package stellar.util;

import arc.util.Strings;

public class StringUtils {
    public static String stripColorsAndGlyphs(String str) {
        str = Strings.stripColors(str);
        return Strings.stripGlyphs(str);
    }

    public static String quote(String in) {
        return in != null ? ("'" + escapeString(in) + "'") : null;
    }

    public static String escapeString(String text) {
        return text.replace("\\", "\\\\").replace("'", "\\'");
    }

    public static String unescapeString(String text) {
//        return text.replace("&quot", "\"").replace("&apos", "'").replace("&amp", "&");
        return text;
    }
}
