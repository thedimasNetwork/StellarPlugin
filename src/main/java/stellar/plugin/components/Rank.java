package stellar.plugin.components;

import arc.graphics.Color;
import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Call;
import mindustry.gen.Iconc;
import mindustry.gen.Player;
import org.jetbrains.annotations.NotNull;
import stellar.database.Database;
import stellar.database.DatabaseAsync;
import stellar.database.enums.PlayerStatus;
import stellar.database.gen.tables.records.StatsRecord;
import stellar.plugin.Variables;
import stellar.plugin.type.Requirements;
import thedimas.util.Bundle;
import stellar.plugin.util.logger.DiscordLogger;

import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static stellar.plugin.Variables.ranks;

public enum Rank {
    // region basic
    player(Fx.none),
    verified(new Requirements(500, 0, 0, 30), player, Fx.none),
    beginner(Color.valueOf("#fc473d"), "\uE872", new Requirements(5000, 50, 8, 60), verified, Fx.lava),
    active(Color.valueOf("#ff8f40"), "\uE86B", new Requirements(7500, 100, 16, 2 * 60), beginner, Fx.bubble, Color.valueOf("#ff8f40")),
    expert(Color.valueOf("#f2e33d"), "\uE86E", new Requirements(10000, 250, 32, 4 * 60), active, Fx.dynamicSpikes, Color.valueOf("#f2e33d")),
    veteran(Color.valueOf("#66f556"), "\uE809", new Requirements(15000, 500, 64, 8 * 60), expert, Fx.greenCloud),
    elite(Color.valueOf("#8cddff"), "\uE80E", new Requirements(30000, 750, 128, 16 * 60), veteran, Fx.vaporSmall, Color.valueOf("#8cddff")),
    master(Color.valueOf("#547cff"), "\uE88B", new Requirements(45000, 1500, 256, 24 * 60), elite, Fx.fluxVapor, Color.valueOf("#547cff")),
    legendary(Color.valueOf("#8148db"), "\uE88E", new Requirements(60000, 3000, 512, 36 * 60), master, Fx.vaporSmall, Color.valueOf("#8148db")),
    // endregion

    // region special
    // TODO: make something with donater/developer
    // TODO: colors for spectial ranks and better owner icon
//    donater(Color.gold, "\uE810", true),
//    developer(Color.orange, "\uE869", true),
    admin(Color.valueOf("#87ceeb"), "\uE817", true), // or \uE82C
    console(Color.valueOf("#6c87fd"), "\uE80F", true),
    owner(Color.valueOf("#a27ce5"), "\uE82C", true);
    //endregion

    private static final ObjectMap<PlayerStatus, Rank> statusRanks = ObjectMap.of( // maybe need some optimization
//            PlayerStatus.basic, null,
            PlayerStatus.admin, Rank.admin,
            PlayerStatus.console, Rank.console,
            PlayerStatus.owner, Rank.owner
    );
    public final Color color;
    public final String icon;
    public final Requirements requirements;
    public final Rank prevRank;
    public final Effect effect;
    public final Color effectColor;
    public final boolean special;

    Rank(Effect effect) {
        this.color = null;
        this.icon = null;
        this.requirements = Requirements.empty();
        this.prevRank = null;
        this.effect = effect;
        this.effectColor = null;
        this.special = false;
    }

    Rank(Requirements requirements, Rank prevRank, Effect effect) {
        this.color = null;
        this.icon = null;
        this.requirements = requirements;
        this.prevRank = prevRank;
        this.effect = effect;
        this.effectColor = null;
        this.special = false;
    }

    Rank(Color color, String icon, Requirements requirements, Effect effect) {
        this.color = color;
        this.icon = icon;
        this.requirements = requirements;
        this.prevRank = null;
        this.effect = effect;
        this.effectColor = null;
        this.special = false;
    }

    Rank(Color color, String icon, Requirements requirements, Rank prevRank, Effect effect) {
        this.color = color;
        this.icon = icon;
        this.requirements = requirements;
        this.prevRank = prevRank;
        this.effect = effect;
        this.effectColor = null;
        this.special = false;
    }

    Rank(Color color, String icon, Requirements requirements, Rank prevRank, Effect effect, Color effectColor) {
        this.color = color;
        this.icon = icon;
        this.requirements = requirements;
        this.prevRank = prevRank;
        this.effect = effect;
        this.effectColor = effectColor;
        this.special = false;
    }

    Rank(Color color, String icon, boolean special) {
        this.color = color;
        this.icon = icon;
        this.requirements = null;
        this.prevRank = null;
        this.effect = null;
        this.effectColor = null;
        this.special = special;
    }

    Rank(Color color, String icon, Rank prevRank, boolean special) {
        this.color = color;
        this.icon = icon;
        this.requirements = null;
        this.prevRank = prevRank;
        this.effect = null;
        this.effectColor = null;
        this.special = special;
    }

    @NotNull
    public static Rank getRank(Requirements requirements) {
        int index = Rank.values().length - 1;
        while (true) {
            Rank rank = Rank.values()[index];

            if (rank.special) {
                index--;
                continue;
            }

            if (rank.prevRank == null) {
                return rank;
            }

            if (rank.requirements.match(requirements)) {
                return rank;
            } else {
                index--;
            }
        }
    }

    @Nullable
    public static Rank getSpecialRank(PlayerStatus playerStatus) {
        return statusRanks.get(playerStatus);
    }

    public static Rank getRank(Player player) throws SQLException {
        if (ranks.containsKey(player.uuid())) {
            return ranks.get(player.uuid());
        }
        return Rank.getRankForced(player);
    }

    public static Rank getRankForced(Player player, Rank oldRank) throws SQLException {
        StatsRecord record = Database.getStats(player.uuid());
        Rank newRank = Rank.getRank(new Requirements(record.getBuilt(), record.getWaves(), record.getAttacks(), record.getSurvivals(), record.getHexWins(), record.getPvp(), (int) (Database.getTotalPlaytime(player.uuid()) / 60)));
        ranks.put(player.uuid(), newRank);
        if (oldRank != null && newRank != oldRank) {
            Log.debug("@ -> @", oldRank.name(), newRank.name());
            Call.warningToast(player.con, Iconc.chartBar, Bundle.format("events.new-rank", Bundle.findLocale(player.locale()), newRank.formatted(player)));
        }
        return newRank;
    }

    public static Rank getRankForced(Player player) throws SQLException {
        Rank oldRank = null;
        if (ranks.containsKey(player.uuid())) {
            oldRank = ranks.get(player.uuid());
        }
        return getRankForced(player, oldRank);
    }

    public static CompletableFuture<Rank> getRankAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (ranks.containsKey(player.uuid())) {
                return ranks.get(player.uuid());
            } else {
                try {
                    return getRankForced(player);
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get rank", e);
                }
            }
        });
    }

    public static CompletableFuture<Rank> getRankForcedAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getRankForced(player);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static CompletableFuture<Rank> getRankForcedAsync(Player player, Rank oldRank) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getRankForced(player, oldRank);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }



    public static Rank max(Rank rank, Rank other) {
        return Rank.values()[Math.max(rank.ordinal(), other.ordinal())];
    }

    @Nullable
    public Rank getNext() {
        for (Rank rank : Rank.values()) {
            if (rank.prevRank == this) {
                return rank;
            }
        }
        return null;
    }

    public String formatted(Player player) {
        Locale locale = Bundle.findLocale(player.locale());
        String bundled = Bundle.get("ranks." + this.name(), locale);
        return icon == null ? bundled :
                String.format("<[#%s]%s[]> %s", this.color, this.icon, bundled);
    }

    // Greater Than or Equal
    public boolean gte(Rank other) {
        return this.ordinal() >= other.ordinal();
    }


    @Override
    public String toString() {
        return "Rank{" +
                "color=" + color +
                ", icon='" + icon + '\'' +
                ", requirements=" + requirements +
                ", prevRank=" + (prevRank != null ? prevRank.name() : "null") +
                '}';
    }
}
