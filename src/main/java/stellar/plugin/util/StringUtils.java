package stellar.plugin.util;

import arc.util.Strings;

import java.util.Base64;
import java.util.Locale;

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

    public static String targetColor(int passed, int required) {
        return passed < required ? "red" : "green";
    }

    public static String longToTime(long seconds, Locale locale) {
        return seconds < 60 * 60 ? Bundle.format("formats.playtime.short", locale, seconds / 60) :
                Bundle.format("formats.playtime", locale, seconds / (60 * 60), (seconds % (60 * 60)) / 60);
    }

    public static String repeat(String string, int number) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < number; i++) {
            builder.append(string);
        }
        return builder.toString();
    }

    public static String obfuscate(String string, boolean escape) {
        return obfuscate(string, 2, escape);
    }

    public static String obfuscate(String string, int chars, boolean escape) {
        return string.substring(0, chars) + repeat(escape ? "\\*" : "*", string.length() - chars * 2) + string.substring(string.length() - chars);
    }
}
