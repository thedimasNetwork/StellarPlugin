package stellar.history.entry;

import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.*;
import mindustry.content.Blocks;
import mindustry.game.EventType.ConfigEvent;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.Door;
import mindustry.world.blocks.power.PowerNode;
import stellar.util.Bundle;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.world;

public class ConfigEntry implements HistoryEntry {

    private StringMap icons;

    public final long timestamp = Time.millis();
    public final String name;
    public final Block block;
    public final Object value;
    public final boolean connect;

    public ConfigEntry(ConfigEvent event, boolean connect) {
        this.name = event.player.coloredName();
        this.block = event.tile.block;
        this.value = getConfig(event);
        this.connect = connect;
    }

    private StringMap getIcons() {
        if (icons != null) {
            return icons;
        }
        Http.get("https://raw.githubusercontent.com/Anuken/Mindustry/v137/core/assets/icons/icons.properties", resp -> {
            for (String line : resp.getResultAsString().split("\n")) {
                String[] arr = line.split("\\|")[0].split("=");
                icons.put(arr[1], String.valueOf((char) Integer.parseInt(arr[0])));
            }
        });
        return icons;
    }

    private Object getConfig(ConfigEvent event) {
        if (block.configurations.containsKey(Integer.class) &&
                (block.configurations.containsKey(Point2[].class) ||
                block.configurations.containsKey(Point2.class))) {
            int count;
            if (block instanceof PowerNode) {
                count = event.tile != null ? event.tile.getPowerConnections(new Seq<>()).size : 0;
            } else {
                count = event.tile != null ? (int) event.value : -1;
            }

            return Pack.longInt(count, (int) event.value);
        }
        return event.value;
    }

    @Override
    public String getMessage(Locale locale) {
        StringMap icons = getIcons();
        if (block.configurations.containsKey(Integer.class) &&
                (block.configurations.containsKey(Point2[].class) || block.configurations.containsKey(Point2.class))) {
            int data = Pack.rightInt((long) value);
            if (data < 0) {
                return Bundle.get("history.config.default", locale);
            }

            Tile tile = world.tile(data);
            if (tile == null) {
                return Bundle.get("history.unknown", locale);
            }

            if (connect) {
                return Bundle.format("history.config.connect", locale, name, block, tile.x, tile.y);
            }

            return Bundle.format("history.config.disconnect", locale, name, block, tile.x, tile.y);
        }

        if (block instanceof Door) {
            boolean data = (boolean) value;
            return Bundle.format(data ? "history.config.open" : "history.config.close", locale, name, block);
        }

        if (block == Blocks.switchBlock) {
            boolean data = (boolean) value;
            return Bundle.format(data ? "history.config.on" : "history.config.off", locale, name);
        }

        /* if (block == Blocks.commandCenter) {
            String[] commands = Bundle.get("history.config.commands", locale).split(",");
            return Bundle.format("history.config.command", locale, name, commands[((UnitCommand) value).ordinal()]);
        } */

        if (block == Blocks.liquidSource) {
            Liquid liquid = (Liquid) value;
            if (liquid == null) {
                return Bundle.format("history.config.default", locale, name);
            }

            return Bundle.format("history.config.update", locale, name, icons.get(liquid.name));
        }

        if (block == Blocks.unloader || block == Blocks.sorter || block == Blocks.invertedSorter || block == Blocks.itemSource) {
            Item item = (Item) value;
            if (item == null) {
                return Bundle.format("history.config.default", locale, name);
            }

            return Bundle.format("history.config.update", locale, name, icons.get(item.name));
        }

        return Bundle.get("history.unknown", locale);
    }

    @Override
    public long getTimestamp(TimeUnit unit) {
        return unit.convert(Time.timeSinceMillis(timestamp), TimeUnit.MILLISECONDS);
    }
}
