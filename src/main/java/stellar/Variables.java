package stellar;

import arc.util.Interval;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import stellar.history.entry.HistoryEntry;
import stellar.history.struct.CacheSeq;
import stellar.history.struct.Seqs;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Variables {
    public static Config config;
    public static final Interval interval = new Interval(2);
    public static int waves = 0;

    public static final Set<String> votesRTV = new HashSet<>();

    // uuid -> enable
    public static final Map<String, Boolean> activeHistoryPlayers = new HashMap<>();

    // uuid -> name
    public static final Map<String, String> admins = new HashMap<>();
    public static final Map<String, String> donaters = new HashMap<>();
    public static final Map<String, String> jsallowed = new HashMap<>();

    public static CacheSeq<HistoryEntry>[][] history;
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
