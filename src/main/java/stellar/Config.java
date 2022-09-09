package stellar;

public class Config {
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
        public boolean main; // used for RPC
    }

    public String webhookUrl;
    public String discordUrl;
    public Database database;
    public Bot bot;
}

