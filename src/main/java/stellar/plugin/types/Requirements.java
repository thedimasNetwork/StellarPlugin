package stellar.plugin.types;

public class Requirements {
    public final int attacks, waves, hexes, built, playtime;

    public Requirements(int attacks, int waves, int hexes, Long long1, int playtime) {
        this.attacks = attacks;
        this.waves = waves;
        this.hexes = hexes;
        this.built = long1.intValue();
        this.playtime = playtime;
    }

    public Requirements(int attacks, int waves, int hexes, int i, int playtime) {
        this.attacks = attacks;
        this.waves = waves;
        this.hexes = hexes;
        this.built = i;
        this.playtime = playtime;
    }

    public boolean match(Requirements requirements) {
        return requirements.attacks >= this.attacks &&
                requirements.waves >= this.waves &&
                requirements.hexes >= this.hexes &&
                requirements.built >= this.built &&
                requirements.playtime >= this.playtime;
    }

    @Override
    public String toString() {
        return String.format("a%s;w%s;h%s;b%s;p%s", attacks, waves, hexes, built, playtime);
    }
}
