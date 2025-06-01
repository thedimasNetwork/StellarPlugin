package stellar.plugin;

import arc.struct.*;
import arc.util.Interval;
import arc.util.Timekeeper;
import mindustry.core.NetServer;
import okhttp3.OkHttpClient;
import stellar.plugin.components.Rank;
import stellar.plugin.components.history.entry.HistoryEntry;
import stellar.plugin.components.history.struct.CacheSeq;
import stellar.plugin.components.history.struct.Seqs;
import stellar.plugin.type.AdminActionEntry;
import stellar.plugin.type.VoteSession;
import stellar.plugin.util.commands.CommandManager;

import java.time.Duration;

public class Variables {
    public static final Interval interval = new Interval(5);
    public static final Seq<String> votesRTV = new Seq<>();
    // uuid -> enable
    public static final ObjectMap<String, Boolean> activeHistoryPlayers = new ObjectMap<>();
    // uuid -> name
    public static final StringMap admins = new StringMap();
    public static final StringMap donaters = new StringMap();
    public static final Seq<String> blacklistedSubnets = new Seq<>();
    public static final IntMap<AdminActionEntry> adminActions = new IntMap<>();
    public static final ObjectMap<String, ObjectMap<String, Integer>> statsData = new ObjectMap<>(); // uuid -> [field -> increase,...]
    public static final IntIntMap unitPlayer = new IntIntMap();
    public static final ObjectMap<String, Rank> ranks = new ObjectMap<>();
    public static final ObjectMap<String, Rank> specialRanks = new ObjectMap<>();
    public static Config config;
    public static CommandManager commandManager = new CommandManager();
    public static CacheSeq<HistoryEntry>[][] history = new CacheSeq[][]{};
    public static OkHttpClient httpClient = new OkHttpClient();
    public static VoteSession voteSession;
    public static ObjectMap<String, Timekeeper> voteCooldowns = new ObjectMap<>();
    public static BoolSeq skippedMaps = new BoolSeq();

    public static CacheSeq<HistoryEntry> getHistorySeq(int x, int y) {
        CacheSeq<HistoryEntry> seq = history[x][y];
        if (seq == null) {
            history[x][y] = seq = Seqs.newBuilder()
                    .maximumSize(8)
                    .expireAfterWrite(Duration.ofMillis(1800000L))
                    .build();
        }
        return seq;
    }
}
