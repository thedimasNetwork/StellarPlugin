package stellar;

import arc.files.Fi;
import arc.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public String webhookUrl;
    public String discordUrl;
    public String pcToken;
    public Database database;
    public Bot bot;

    public static void load() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        if (!new Fi(Const.PLUGIN_FOLDER).exists()) {
            try {
                Files.createDirectory(Path.of(Const.PLUGIN_FOLDER));
            } catch (IOException e) {
                Log.err(e);
            }
        }

        if (!new Fi(Const.PLUGIN_FOLDER + "plugin.yaml").exists()) {
            try (InputStream is = Config.class.getClassLoader().getResourceAsStream("plugin.yaml")) {
                Files.copy(is, Path.of(Const.PLUGIN_FOLDER + "plugin.yaml"));
            } catch (IOException e) {
                Log.err(e);
            }
        }
        try {
            Variables.config = mapper.readValue(new File(Const.PLUGIN_FOLDER + "plugin.yaml"), Config.class);
        } catch (IOException e) {
            Log.err(e);
        }
    }

    public static class Database {
        public String ip;
        public int port;
        public String user;
        public String password;
        public String name;
    }

    public static class Bot {
        public String token;
        public long channelId;
        public long bansId;
        public long adminId;
        public boolean main; // used for RPC
    }
}
