package stellar.plugin.components.history;

import arc.Events;
import arc.graphics.Color;
import arc.struct.Seq;
import arc.util.Pack;
import mindustry.content.Fx;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.net.Administration;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.power.PowerNode;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.components.history.entry.BlockEntry;
import stellar.plugin.components.history.entry.ConfigEntry;
import stellar.plugin.components.history.entry.HistoryEntry;
import stellar.plugin.components.history.entry.RotateEntry;
import stellar.plugin.components.history.struct.CacheSeq;
import stellar.plugin.util.Bundle;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.*;
import static stellar.plugin.Variables.activeHistoryPlayers;

public class History {
    public static void load() {
        // region история
        netServer.admins.addActionFilter(action -> {
            if (action.type == Administration.ActionType.rotate) {
                HistoryEntry entry = new RotateEntry(action.player, action.tile.build.block, action.rotation);
                Variables.getHistorySeq(action.tile.x, action.tile.y).add(entry);
            }
            return !activeHistoryPlayers.containsKey(action.player.uuid());
        });

        Events.on(EventType.BlockBuildEndEvent.class, event -> {
            if (event.tile.build == null) {
                return; // игнорируем ломание/строительство блоков по типу валунов
            }

            HistoryEntry historyEntry = new BlockEntry(event);
            Seq<Tile> linkedTile = event.tile.getLinkedTiles(new Seq<>());
            for (Tile tile : linkedTile) {
                Variables.getHistorySeq(tile.x, tile.y).add(historyEntry);
            }
        });

        Events.on(EventType.ConfigEvent.class, event -> {
            if (event.tile.block instanceof LogicBlock || event.player == null || event.tile.tileX() > world.width() || event.tile.tileY() > world.height()) {
                return;
            }

            CacheSeq<HistoryEntry> entries = Variables.getHistorySeq(event.tile.tileX(), event.tile.tileY());
            boolean connect = true;

            HistoryEntry last = entries.peek();
            if (!entries.isEmpty() && last instanceof ConfigEntry lastConfigEntry) {
                Seq<Building> conns = event.tile.getPowerConnections(new Seq<>());
                connect = lastConfigEntry.value instanceof Long &&
                        (conns.any() && event.tile.block instanceof PowerNode &&
                                conns.size > Pack.leftInt((Long) lastConfigEntry.value) ||
                                event.value instanceof Integer && (int) event.value >= 0);
            }

            HistoryEntry entry = new ConfigEntry(event, connect);

            Seq<Tile> linkedTile = event.tile.tile.getLinkedTiles(new Seq<>());
            for (Tile tile : linkedTile) {
                Variables.getHistorySeq(tile.x, tile.y).add(entry);
            }
        });

        Events.on(EventType.TapEvent.class, event -> {
            if (Variables.activeHistoryPlayers.containsKey(event.player.uuid())) {
                int x = event.tile.x;
                int y = event.tile.y;
                Call.effect(event.player.con, Fx.tapBlock, event.tile.x * tilesize, event.tile.y * tilesize, 0, Color.white);

                CacheSeq<HistoryEntry> entries = Variables.getHistorySeq(x, y);

                boolean detailed = Variables.activeHistoryPlayers.get(event.player.uuid());

                StringBuilder result = new StringBuilder();
                Locale locale = Bundle.findLocale(event.player.locale);
                result.append(Bundle.format("history.page", locale, x, y)).append("\n");

                entries.cleanUp();
                if (entries.isEmpty()) {
                    result.append(Bundle.get("history.empty", locale)).append("\n");
                }

                for (int i = 0; i < entries.size && i < Const.listPageSize; i++) {
                    HistoryEntry entry = entries.get(i);

                    result.append(entry.getMessage(locale));
                    if (detailed) {
                        result.append(Bundle.format("history.timestamp", locale, entry.getTimestamp(TimeUnit.MINUTES)));
                    }
                    result.append("\n");
                }

                event.player.sendMessage(result.toString());
            }
        });
        // endregion
    }
}
