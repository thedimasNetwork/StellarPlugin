package stellar.history.entry;

import arc.util.Time;
import mindustry.gen.Player;
import mindustry.world.Block;
import stellar.util.Bundle;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RotateEntry implements HistoryEntry {

    protected static final String[] sides = {"\uE803", "\uE804", "\uE802", "\uE805"};

    public final String name;
    public final Block block;
    public final int rotation;
    public long timestamp = Time.millis();

    public RotateEntry(Player player, Block block, int rotation) {
        this.name = player.coloredName();
        this.block = block;
        this.rotation = rotation;
    }

    @Override
    public String getMessage(Locale locale) {
        return Bundle.format("history.rotate.text", locale, name, block.name, sides[rotation]);
    }

    @Override
    public long getTimestamp(TimeUnit unit) {
        return unit.convert(Time.timeSinceMillis(timestamp), TimeUnit.MILLISECONDS);
    }
}
