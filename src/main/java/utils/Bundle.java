package utils;

import arc.files.Fi;
import arc.util.Log;
import arc.util.io.Streams;
import main.ThedimasPlugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Bundle {

    private static final Properties props = new Properties();

    public Bundle() {
        Fi propsFile = Fi.get("config/mods/thedimasPlugin/lang.properties");
        if (!propsFile.exists()) {
            try {
                Streams.copy(ThedimasPlugin.class.getClassLoader().getResourceAsStream(propsFile.name()), propsFile.write());
            } catch (Throwable t) {
                Log.err("Failed to copy 'lang.properties'", t);
            }
        }
        reload();
    }

    public void reload() {
        Fi bundle = Fi.get("config/mods/thedimasPlugin/lang.properties");
        try {
            props.load(new InputStreamReader(bundle.read(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.err(e.getMessage());
        }
    }

    public String get(String key, Object... replace) {
        String value = props.getProperty(key);
        for (int i = 0; i < replace.length; i++) {
            value = value.replace("{" + i + "}", String.valueOf(replace[i]));
        }
        return value;
    }
}
