package util;

import arc.struct.ObjectMap;
import arc.util.*;
import main.Const;

import java.text.MessageFormat;
import java.util.*;

public class Bundle {

    private static final ObjectMap<Locale, ResourceBundle> bundles = new ObjectMap<>();

    private static final ObjectMap<Locale, MessageFormat> formats = new ObjectMap<>();

    private Bundle() {}

    public static String get(String key, Locale locale) {
        try {
            ResourceBundle bundle = getOrLoad(locale);
            return bundle.containsKey(key) ? bundle.getString(key) : "???" + key + "???";
        } catch (MissingResourceException t) {
            return key;
        }
    }

    public static boolean has(String key, Locale locale) {
        return getOrLoad(locale).containsKey(key);
    }

    public static String format(String key, Locale locale, Object... values) {
        String pattern = get(key, locale);
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

    private static ResourceBundle getOrLoad(Locale locale) {
        ResourceBundle bundle = bundles.get(locale);
        if (bundle == null && Structs.contains(Const.supportedLocales, locale)) {
            bundles.put(locale, bundle = ResourceBundle.getBundle("bundles.bundle", locale));
        }
        return bundle != null ? bundle : bundles.get(Const.defaultLocale());
    }
}
