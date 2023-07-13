package stellar.plugin.components;

import arc.graphics.Color;
import stellar.plugin.types.Requirements;
import mindustry.content.Fx;
import mindustry.entities.Effect;

public enum Rank {
    player(Fx.burning),
    beginner(Color.valueOf("#fc693d"), "\uE872", new Requirements(5, 10, 2, 200, 1 * 60), player, Fx.bubble),
    active(Color.valueOf("#ff8f40"), "\uE86B", new Requirements(8, 25, 3, 500, 2 * 60), beginner, Fx.chainLightning),
    expert(Color.valueOf("#f2e33d"), "\uE86E", new Requirements(12, 50, 5, 1000, 4 * 60), active, Fx.electrified),
    veteran(Color.valueOf("#66f556"), "\uE809", new Requirements(15, 100, 8, 2000, 8 * 60), expert, Fx.absorb),
    elite(Color.valueOf("#8cddff"), "\uE80E", new Requirements(20, 200, 12, 5000, 16 * 60), veteran, Fx.oily),
    master(Color.valueOf("#547cff"), "\uE88B", new Requirements(30, 400, 16, 10000, 24 * 60), elite, Fx.missileTrailShort),
    legendary(Color.valueOf("#8148db"), "\uE88E", new Requirements(40, 600, 20, 20000, 36 * 60), master, Fx.ballfire);


    public final Color color;
    public final String icon;
    public final Requirements requirements;
    public final Rank prevRank;
    public final Effect effect;

    Rank(Effect effect) {
        this.color = null;
        this.icon = null;
        this.requirements = new Requirements(0, 0, 0, 0, 0);
        this.prevRank = null;
        this.effect = effect;
    }

    Rank(Color color, String icon, Requirements requirements, Effect effect) {
        this.color = color;
        this.icon = icon;
        this.requirements = requirements;
        this.prevRank = null;
        this.effect = effect;
    }

    Rank(Color color, String icon, Requirements requirements, Rank prevRank, Effect effect) {
        this.color = color;
        this.icon = icon;
        this.requirements = requirements;
        this.prevRank = prevRank;
        this.effect = effect;
    }

    public Rank getNext() {
        for (Rank rank : Rank.values()) {
            if (rank.prevRank == this) {
                return rank;
            }
        }
        return null;
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
}
