package stellar.plugin.components;

import arc.graphics.Color;
import stellar.plugin.types.Requirements;

public enum Rank {
    player,
    beginner(Color.valueOf("#fc693d"), "\uE872", new Requirements(5, 10, 2, 200, 1 * 60), player),
    active(Color.valueOf("#ff8f40"), "\uE86B", new Requirements(8, 25, 3, 500, 2 * 60), beginner),
    expert(Color.valueOf("#f2e33d"), "\uE86E", new Requirements(12, 50, 5, 1000, 4 * 60), active),
    veteran(Color.valueOf("#66f556"), "\uE809", new Requirements(15, 100, 8, 2000, 8 * 60), expert),
    elite(Color.valueOf("#8cddff"), "\uE80E", new Requirements(20, 200, 12, 5000, 16 * 60), veteran),
    master(Color.valueOf("#547cff"), "\uE88B", new Requirements(30, 400, 16, 10000, 24 * 60), elite),
    legendary(Color.valueOf("#8148db"), "\uE88E", new Requirements(40, 600, 20, 20000, 36 * 60), master);


    public final Color color;
    public final String icon;
    public final Requirements requirements;
    public final Rank prevRank;

    Rank() {
        this.color = null;
        this.icon = null;
        this.requirements = new Requirements(0, 0, 0, 0, 0);
        this.prevRank = null;
    }

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
