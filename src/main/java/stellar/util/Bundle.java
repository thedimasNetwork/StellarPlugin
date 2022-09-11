package stellar.util;

import arc.struct.*;
import arc.util.*;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import stellar.Const;

import java.text.MessageFormat;
import java.util.*;

public class Bundle {

    private static final ObjectMap<Locale, StringMap> bundles = new ObjectMap<>();

    private static final ObjectMap<Locale, MessageFormat> formats = new ObjectMap<>();

    private Bundle() {}

    public static String get(String key, Locale locale) {
        StringMap bundle = getOrLoad(locale);
        return bundle.containsKey(key) ? bundle.get(key) : "???" + key + "???";
    }

    public static boolean has(String key, Locale locale) {
        StringMap props = getOrLoad(locale);
        return props.containsKey(key);
    }

    public static String format(String key, Locale locale, Object... values) {
        String pattern = get(key, locale);
        if (values.length == 0) {
            return pattern;
        }

        MessageFormat format = formats.get(locale);
        if (!Structs.contains(Const.supportedLocales, locale)) {
            format = formats.get(Const.defaultLocale(), () -> new MessageFormat(pattern, Const.defaultLocale()));
            format.applyPattern(pattern);
        } else if (format == null) {
            format = new MessageFormat(pattern, locale);
            formats.put(locale, format);
        } else {
            format.applyPattern(pattern);
        }
        return format.format(values);
    }

    private static StringMap getOrLoad(Locale locale) {
        StringMap bundle = bundles.get(locale);
        if (bundle == null) {
            if (locale.getDisplayName().equals("router")) {
                StringMap router = new StringMap();
                getOrLoad(Const.defaultLocale()).each((k, v) -> router.put(k, "router"));
                bundles.put(locale, bundle = router);
            } else if (Structs.contains(Const.supportedLocales, locale)) {
                bundles.put(locale, bundle = load(locale));
            } else {
                bundle = getOrLoad(Const.defaultLocale());
            }
        }
        return bundle;
    }

    private static StringMap load(Locale locale) {
        StringMap properties = new StringMap();
        ResourceBundle bundle = ResourceBundle.getBundle("bundles.bundle", locale);
        for (String s : bundle.keySet()) {
            properties.put(s, bundle.getString(s));
        }
        return properties;
    }

    public static void bundled(Player player, boolean condition, String keyTrue, String keyFalse, Object... values) {
        String key = condition ? keyTrue : keyFalse;
        player.sendMessage(Bundle.format(key, findLocale(player.locale), values));
    }

    public static void bundled(Player player, String key, Object... values) {
        player.sendMessage(Bundle.format(key, findLocale(player.locale), values));
    }

    public static void bundled(String key, Object... values) {
        Groups.player.each(p -> bundled(p, key, values));
    }

    public static Locale parseLocale(String code) {
        if (code.contains("_")) {
            String[] codes = code.split("_");
            return new Locale(codes[0], codes[1]);
        }
        return new Locale(code);
    }

    public static Locale findLocale(String code) {
        Locale locale = Structs.find(Const.supportedLocales, l -> l.toString().equals(code) ||
                code.startsWith(l.toString()));
        return locale != null ? locale : Const.defaultLocale();
    }
}
