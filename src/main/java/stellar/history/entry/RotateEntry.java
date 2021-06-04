package stellar.history.entry;

import arc.util.Time;
import mindustry.core.NetClient;
import mindustry.gen.Player;
import mindustry.world.Block;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

public class RotateEntry implements HistoryEntry {

    protected static final String[] sides = {"\uE803", "\uE804", "\uE802", "\uE805"};

    public final String name;
    public final Block block;
    public final int rotation;
    public long lastAccessTime = Time.millis();

    public RotateEntry(Player player, Block block, int rotation) {
        this.name = NetClient.colorizeName(player.id, player.name);
        this.block = block;
        this.rotation = rotation;
    }

    @Override
    public String getMessage() {
        return MessageFormat.format("[slate]\uE823 {0}[lightgray] повернул [orange]{1} [orange]{2}", name, block.name, sides[rotation]);
    }

    @Override
    public long getLastAccessTime(TimeUnit unit) {
        return unit.convert(Time.timeSinceMillis(lastAccessTime), TimeUnit.MILLISECONDS);
    }
}