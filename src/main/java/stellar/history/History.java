package stellar.history;

import arc.Events;
import arc.struct.Seq;
import arc.util.Pack;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.net.Administration;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.power.PowerNode;
import stellar.Const;
import stellar.Variables;
import stellar.history.entry.BlockEntry;
import stellar.history.entry.ConfigEntry;
import stellar.history.entry.HistoryEntry;
import stellar.history.entry.RotateEntry;
import stellar.history.struct.CacheSeq;
import stellar.util.Bundle;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.netServer;
import static mindustry.Vars.world;

public class History {
    public static void load() {
        // region история
        netServer.admins.addActionFilter(action -> {
            if (action.type == Administration.ActionType.rotate) {
                HistoryEntry entry = new RotateEntry(action.player, action.tile.build.block, action.rotation);
                Variables.getHistorySeq(action.tile.x, action.tile.y).add(entry);
            }
            return true;
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
            if (!entries.isEmpty() && last instanceof ConfigEntry) {
                ConfigEntry lastConfigEntry = (ConfigEntry) last;

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

                CacheSeq<HistoryEntry> entries = Variables.getHistorySeq(x, y);

                boolean detailed = Variables.activeHistoryPlayers.get(event.player.uuid());

                StringBuilder result = new StringBuilder();
                Locale locale = Bundle.findLocale(event.player.locale);
                result.append(Bundle.format("history.page", locale, x, y)).append("\n");

                entries.cleanUp();
                if (entries.isEmpty()) {
                    result.append(Bundle.get("history.empty", locale)).append("\n");
                }

                for (int i = 0; i < entries.size && i < Const.LIST_PAGE_SIZE; i++) {
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
