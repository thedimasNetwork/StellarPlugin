package stellar.plugin;

import arc.struct.IntIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Interval;
import stellar.plugin.history.entry.HistoryEntry;
import stellar.plugin.history.struct.CacheSeq;
import stellar.plugin.history.struct.Seqs;
import stellar.plugin.types.AdminActionEntry;

import java.time.Duration;

public class Variables {
    public static final Interval interval = new Interval(5);
    public static final Seq<String> votesRTV = new Seq<>();
    // uuid -> enable
    public static final ObjectMap<String, Boolean> activeHistoryPlayers = new ObjectMap<>();
    // uuid -> name
    public static final StringMap admins = new StringMap();
    public static final StringMap donaters = new StringMap();
    public static final StringMap jsallowed = new StringMap();
    public static Config config;
    public static CacheSeq<HistoryEntry>[][] history = new CacheSeq[2048][2048];
    public static final Seq<String> blacklistedSubnets = new Seq<>();
    public static final ObjectMap<Integer, AdminActionEntry> adminActions = new ObjectMap<>();
    public static final ObjectMap<String, ObjectMap<String, Integer>> statsData = new ObjectMap<>(); // uuid -> [field -> increase,...]
    public static final IntIntMap unitPlayer = new IntIntMap();

    public static CacheSeq<HistoryEntry> getHistorySeq(int x, int y) {
        CacheSeq<HistoryEntry> seq = history[x][y];
        if (seq == null) {
            history[x][y] = seq = Seqs.newBuilder()
                    .maximumSize(15)
                    .expireAfterWrite(Duration.ofMillis(1800000L))
                    .build();
        }
        return seq;
    }
}
