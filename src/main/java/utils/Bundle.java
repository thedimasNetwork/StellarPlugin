package utils;

import arc.files.Fi;
import arc.util.Log;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

public class Bundle {
    private static Properties props = new Properties();
    public Bundle() {
        if(!Fi.get("config/mods/thedimasPlugin/lang.properties").exists()) {
            ResourceCopy.export("lang.properties");
        }
        reload();
    }
    public void reload() {
        Fi bundle = Fi.get("config/mods/thedimasPlugin/lang.properties");
        try {
            props.load(bundle.read());
        } catch (IOException e) {
            Log.err(e.getMessage());
        }
    }
    public static String get(String key, String... replace) {
        String value = props.getProperty(key);
        int i = 0;
        for (String to : replace) {
            value = value.replace("{" + i + "}", to);
            i++;
        }

        return value;
    }
}
