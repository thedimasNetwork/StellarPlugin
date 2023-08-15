package stellar.plugin.components.history.entry;

import arc.math.geom.Point2;
import arc.struct.StringMap;
import arc.util.*;
import mindustry.ctype.MappableContent;
import mindustry.game.EventType.ConfigEvent;
import mindustry.world.Block;
import mindustry.world.blocks.logic.CanvasBlock;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.power.LightBlock;
import mindustry.world.blocks.units.UnitFactory;
import thedimas.util.Bundle;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ConfigEntry implements HistoryEntry {

    public final long timestamp = Time.millis();
    public final String name;
    public final Block block;
    public final Object value;
    public final boolean connect;
    private final StringMap icons = new StringMap();

    public ConfigEntry(ConfigEvent event, boolean connect) {
        Http.get("https://raw.githubusercontent.com/Anuken/Mindustry/v145/core/assets/icons/icons.properties", resp -> {
            for (String line : resp.getResultAsString().split("\n")) {
                String[] arr = line.split("\\|")[0].split("=");
                icons.put(arr[1], String.valueOf((char) Strings.parseInt(arr[0])));
            }
        });
        this.name = event.player.coloredName();
        this.block = event.tile.block;
        this.value = getConfig(event);
        this.connect = connect;
    }

    private Object getConfig(ConfigEvent event) {
        if (event.value instanceof Integer) {
            int number = (int) event.value;
            if (event.tile.block instanceof UnitFactory factory) {
                return number == -1 ? null : factory.plans.get(number).unit;
            }

            if (event.tile.block.configurations.containsKey(Point2.class) || event.tile.block.configurations.containsKey(Point2[].class)) {
                return Point2.unpack(number);
            }
        }

        if (event.value instanceof Point2 point) {
            return point.add(event.tile.tileX(), event.tile.tileY());
        }

        if (event.value instanceof Point2[] points) {
            Structs.each(point -> point.add(event.tile.tileX(), event.tile.tileY()), points);
            return points;
        }
        return event.value;
    }

    @Override
    public String getMessage(Locale locale) {
        if (value instanceof MappableContent content) {
            return Bundle.format("history.config.update", locale, name, icons.get(content.name));
        }

        if (value instanceof Boolean on) {
            return on ?
                    Bundle.format("history.config.on", locale, name) :
                    Bundle.format("history.config.off", locale, name);
        }

        if (value instanceof String text) {
            return !text.isEmpty() ?
                    Bundle.format("history.config.text", locale, name, text) :
                    Bundle.format("history.config.default", locale, name);
        }

        if (value instanceof Point2 point) {
            return connect ?
                    Bundle.format("history.config.connect", locale, name, point.x, point.y) :
                    Bundle.format("history.config.disconnect", locale, name);
        }

        if (value instanceof Point2[] points) {
            return points.length > 0 ?
                    Bundle.format("history.config.connects", locale, name, Arrays.toString(points)) :
                    Bundle.format("history.config.disconnect", locale, name);
        }

        if (block instanceof LightBlock) {
            return Bundle.format("history.config.color", locale, name, Tmp.c1.set((int) value));
        }

        if (block instanceof LogicBlock) {
            return Bundle.format("history.config.code", locale, name);
        }

        if (block instanceof CanvasBlock) {
            return Bundle.format("history.config.image", locale, name);
        }
        return Bundle.get("history.unknown", locale);
    }

    @Override
    public long getTimestamp(TimeUnit unit) {
        return unit.convert(Time.timeSinceMillis(timestamp), TimeUnit.MILLISECONDS);
    }
}
