package stellar.plugin.type;

public class Requirements {
    public final int built, waves, wins, playtime;

    public Requirements(int built, int waves, int wins, int playtime) {
        this.built = built;
        this.waves = waves;
        this.wins = wins;
        this.playtime = playtime;
    }

    public Requirements(int built, int waves, int attack, int survival, int hex, int pvp, int playtime) {
        this.built = built;
        this.waves = waves;
        this.wins = attack + survival + hex + pvp;
        this.playtime = playtime;
    }

    public static Requirements empty() {
        return new Requirements(0, 0, 0, 0);
    }

    public boolean match(Requirements requirements) {
        return requirements.built >= this.built &&
                requirements.waves >= this.waves &&
                requirements.wins >= this.wins &&
                requirements.playtime >= this.playtime;
    }
}
