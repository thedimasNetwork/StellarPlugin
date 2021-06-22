package stellar.util;

import arc.struct.*;
import arc.util.*;
import stellar.Const;

import java.text.MessageFormat;
import java.util.*;

public class Bundle {

    private static final ObjectMap<Locale, StringMap> bundles = new ObjectMap<>();

    private static final ObjectMap<Locale, MessageFormat> formats = new ObjectMap<>();

    private Bundle() {}

    public static String get(String key, Locale locale) {
        StringMap bundle = getOrLoad(locale);
        return bundle != null && bundle.containsKey(key) ? bundle.get(key) : "???" + key + "???";
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

    private static StringMap getOrLoad(Locale locale) {
        StringMap bundle = bundles.get(locale);
        if (bundle == null && locale.getDisplayName().equals("router")) { // router
            StringMap router = new StringMap();
            getOrLoad(Const.defaultLocale()).each((k, v) -> router.put(k, "router"));
            bundles.put(locale, bundle = router);
        } else if (bundle == null && Structs.contains(Const.supportedLocales, locale)) {
            bundles.put(locale, bundle = load(locale));
        }
        return bundle != null ? bundle : bundles.get(Const.defaultLocale());
    }

    private static StringMap load(Locale locale) {
        StringMap properties = new StringMap();
        ResourceBundle bundle = ResourceBundle.getBundle("bundle", locale);
        for (String s : bundle.keySet()) {
            properties.put(s, bundle.getString(s));
        }
        return properties;
    }
}
