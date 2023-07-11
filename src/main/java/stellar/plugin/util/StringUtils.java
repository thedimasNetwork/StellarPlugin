package stellar.plugin.util;

import arc.util.Strings;

import java.util.Base64;

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

    public static boolean isBase64(String input) {
        try {
            Base64.getDecoder().decode(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String fancyBool(boolean bool) {
        return bool ? "✔" : "✘";
    }
}
