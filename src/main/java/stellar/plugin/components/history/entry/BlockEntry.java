package stellar.plugin.components.history.entry;

import arc.util.Nullable;
import arc.util.Time;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.world.Block;
import stellar.plugin.util.Bundle;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BlockEntry implements HistoryEntry {

    public final long timestamp = Time.millis();
    @Nullable
    public final String name;
    public final Unit unit;
    public final Block block;
    public final boolean breaking;
    public final int rotation;

    public BlockEntry(BlockBuildEndEvent event) {
        this.unit = event.unit;
        Player player = unit.isPlayer() ? unit.getPlayer() : null;
        this.name = player != null ? player.coloredName() : null;
        this.block = event.tile.build.block;
        this.breaking = event.breaking;
        this.rotation = event.tile.build.rotation;
    }

    @Override
    public String getMessage(Locale locale) {
        if (breaking) {
            return name != null ? Bundle.format("history.block.broke", locale, name) :
                    Bundle.format("history.block.unit.broke", locale, unit.type);
        }

        String base = name != null ? Bundle.format("history.block.build", locale, name, block) :
                Bundle.format("history.block.unit.build", locale, unit.type, block);
        if (block.rotate) {
            base += Bundle.format("history.block.rotate", locale, RotateEntry.sides[rotation]);
        }
        return base;
    }

    @Override
    public long getTimestamp(TimeUnit unit) {
        return unit.convert(Time.timeSinceMillis(timestamp), TimeUnit.MILLISECONDS);
    }
}
