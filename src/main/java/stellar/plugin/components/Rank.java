package stellar.plugin.components;

import arc.graphics.Color;
import mindustry.gen.Player;
import stellar.database.Database;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.plugin.Variables;
import stellar.plugin.types.Requirements;
import mindustry.content.Fx;
import mindustry.entities.Effect;

import stellar.plugin.util.Bundle;
import stellar.plugin.util.Players;

import java.sql.SQLException;
import java.util.Locale;

public enum Rank {
    player(Fx.burning),
    beginner(Color.valueOf("#fc693d"), "\uE872", new Requirements(5, 10, 2, 200, 1 * 60), player, Fx.lava),
    active(Color.valueOf("#ff8f40"), "\uE86B", new Requirements(8, 25, 3, 500, 2 * 60), beginner, Fx.bubble, Color.valueOf("#ff8f40")),
    expert(Color.valueOf("#f2e33d"), "\uE86E", new Requirements(12, 50, 5, 1000, 4 * 60), active, Fx.dynamicSpikes, Color.valueOf("#f2e33d")),
    veteran(Color.valueOf("#66f556"), "\uE809", new Requirements(15, 100, 8, 2000, 8 * 60), expert, Fx.electrified),
    elite(Color.valueOf("#8cddff"), "\uE80E", new Requirements(20, 200, 12, 5000, 16 * 60), veteran, Fx.freezing),
    master(Color.valueOf("#547cff"), "\uE88B", new Requirements(30, 400, 16, 10000, 24 * 60), elite, Fx.fluxVapor, Color.valueOf("#547cff")),
    legendary(Color.valueOf("#8148db"), "\uE88E", new Requirements(40, 600, 20, 20000, 36 * 60), master, Fx.vaporSmall, Color.valueOf("#8148db"));


    public final Color color;
    public final String icon;
    public final Requirements requirements;
    public final Rank prevRank;
    public final Effect effect;
    public final Color effectColor;

    Rank(Effect effect) {
        this.color = null;
        this.icon = null;
        this.requirements = new Requirements(0, 0, 0, 0, 0);
        this.prevRank = null;
        this.effect = effect;
        this.effectColor = null;
    }

    Rank(Color color, String icon, Requirements requirements, Effect effect) {
        this.color = color;
        this.icon = icon;
        this.requirements = requirements;
        this.prevRank = null;
        this.effect = effect;
        this.effectColor = null;
    }

    Rank(Color color, String icon, Requirements requirements, Rank prevRank, Effect effect) {
        this.color = color;
        this.icon = icon;
        this.requirements = requirements;
        this.prevRank = prevRank;
        this.effect = effect;
        this.effectColor = null;
    }

    Rank(Color color, String icon, Requirements requirements, Rank prevRank, Effect effect, Color effectColor) {
        this.color = color;
        this.icon = icon;
        this.requirements = requirements;
        this.prevRank = prevRank;
        this.effect = effect;
        this.effectColor = effectColor;
    }

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

    public static Rank getRank(Requirements requirements) {
        Rank rank = Rank.values()[Rank.values().length - 1];
        while (true) {
            if (rank.prevRank == null) {
                return rank;
            }

            if (rank.requirements.match(requirements)) {
                return rank;
            } else {
                rank = rank.prevRank;
            }
        }
    }

    public static Rank getRankForced(Player player) throws SQLException {
        UsersRecord record = Database.getPlayer(player.uuid());
        return Rank.getRank(new Requirements(record.getAttacks(), record.getWaves(), record.getHexes(), record.getBuilt(), (int) (Players.totalPlaytime(player.uuid()) / 60)));
    }

    public static Rank getRank(Player player) throws SQLException {
        if (Variables.ranks.containsKey(player.uuid())) {
            return Variables.ranks.get(player.uuid());
        }
        Rank rank = Rank.getRankForced(player);
        Variables.ranks.put(player.uuid(), rank);
        return rank;
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
