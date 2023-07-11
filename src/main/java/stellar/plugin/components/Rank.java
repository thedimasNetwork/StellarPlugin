package stellar.plugin.components;

import arc.graphics.Color;
import stellar.plugin.types.Requirements;

public enum Rank {
    player(Color.acid, "-", new Requirements(0, 0, 0, 0, 0)),
    beginner(Color.acid, "A", new Requirements(10, 0, 0, 0, 1), player),
    active(Color.acid, "B", new Requirements(15, 5, 0, 0, 2), beginner);

    public final Color color;
    public final String icon;
    public final Requirements requirements;
    public final Rank prevRank;

    Rank(Color color, String icon, Requirements requirements) {
        this.color = color;
        this.icon = icon;
        this.requirements = requirements;
        this.prevRank = null;
    }

    Rank(Color color, String icon, Requirements requirements, Rank prevRank) {
        this.color = color;
        this.icon = icon;
        this.requirements = requirements;
        this.prevRank = prevRank;
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
