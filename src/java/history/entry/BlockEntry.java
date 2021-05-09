package history.entry;

import arc.util.Nullable;
import arc.util.Time;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.world.Block;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

public class BlockEntry implements HistoryEntry {

    public final long lastAccessTime = Time.millis();
    @Nullable
    public final String name;
    public final Unit unit;
    public final Block block;
    public final boolean breaking;
    public final int rotation;

    public BlockEntry(BlockBuildEndEvent event) {
        this.unit = event.unit;
        this.name = unit.isPlayer() ? unit.getPlayer().name : unit.controller() instanceof Player ? unit.getPlayer().name : null;
        this.block = event.tile.build.block;
        this.breaking = event.breaking;
        this.rotation = event.tile.build.rotation;
    }

    @Override
    public String getMessage() {
        if (breaking) {
            return name != null ? MessageFormat.format("[scarlet]- {0}[lightgray] сломал этот блок", name) :
                    MessageFormat.format("[scarlet]- [lightgray]юнит [orange]{0}[lightgray] сломал этот блок", unit.type);
        }
        String base = name != null ? MessageFormat.format("[green]+ {0}[lightgray] поставил [orange]{1}", name, block) :
                MessageFormat.format("[green]+ [lightgray]юнит [orange]{0}[lightgray] поставил [lightgray]{1}", unit.type, block);
        if (block.rotate) {
            base += MessageFormat.format("[lightgray] ([orange]{0}[lightgray])", RotateEntry.sides[rotation]);
        }
        return base;
    }

    @Override
    public long getLastAccessTime(TimeUnit unit) {
        return unit.convert(Time.timeSinceMillis(lastAccessTime), TimeUnit.MILLISECONDS);
    }
}
