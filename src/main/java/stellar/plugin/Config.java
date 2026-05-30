package stellar.plugin;

import arc.files.Fi;
import arc.util.Log;
import arc.util.Nullable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jooq.Field;
import stellar.database.gen.Tables;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Config {
    public String webhookUrl;
    public String discordUrl;
    public String pcToken;
    public String serverName;

    public Database database;
    public Bot bot;
    public ServerInfo[] servers;

    public static void load() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        if (!new Fi(Const.pluginFolder).exists()) {
            try {
                Files.createDirectory(Path.of(Const.pluginFolder));
            } catch (IOException e) {
                Log.err(e);
            }
        }

        if (!new Fi(Const.pluginFolder + "plugin.yaml").exists()) {
            try (InputStream is = Config.class.getClassLoader().getResourceAsStream("plugin.yaml")) {
                Files.copy(is, Path.of(Const.pluginFolder + "plugin.yaml"));
            } catch (IOException e) {
                Log.err(e);
            }
        }
        try {
            Variables.config = mapper.readValue(new File(Const.pluginFolder + "plugin.yaml"), Config.class);
            Variables.playtimeField = (Field<Long>) Tables.playtime.field(Variables.config.serverName);
        } catch (IOException e) {
            Log.err(e);
        }
    }

    @Nullable
    public ServerInfo findServer(String query) {
        return Arrays.stream(servers)
                .filter(s -> s.name.equalsIgnoreCase(query) || s.id.equalsIgnoreCase(query))
                .findAny()
                .orElse(null);
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

    public static class ServerInfo {
        public String id;
        public char emoji;
        public String name;
        public String ip; // ip:port

        public String getDomain() {
            return ip.split(":")[0];
        }

        public int getPort() {
            String[] split = ip.split(":");
            if (split.length == 1) return 6567;
            return Integer.parseInt(split[1]);
        }
    }
}
