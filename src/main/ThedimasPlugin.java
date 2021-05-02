package main;

import arc.*;
import arc.struct.Seq;
import main.history.entry.BlockEntry;
import main.history.entry.ConfigEntry;
import main.history.entry.RotateEntry;
import main.history.struct.Seqs;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.mod.*;
import arc.util.*;
import mindustry.net.Administration;
import mindustry.type.UnitType;

import main.history.struct.CacheSeq;
import main.history.entry.HistoryEntry;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;

import static mindustry.Vars.netServer;
import static mindustry.Vars.world;

@SuppressWarnings({"unused", "unchecked"})
public class ThedimasPlugin extends Plugin {

    private final Interval interval = new Interval();

    private CacheSeq<HistoryEntry>[][] history;

    //called when game initializes
    @Override
    public void init() {
        Log.info("thedimasPlugin launched!");

        Events.on(EventType.PlayerJoin.class, event -> {
            Log.info(MessageFormat.format(Const.JOIN_LOG_FORMAT, event.player.name, event.player.locale, event.player.con.address));
            Call.sendMessage("[lime]+ [accent]" + event.player.name + "[lime] присоединился");

            if (event.player.locale.startsWith("uk")) {
                Call.infoMessage(event.player.con, Const.WELCOME_UK);
            } else if (event.player.locale.startsWith("ru")) {
                Call.infoMessage(event.player.con, Const.WELCOME_RU);
            } else {
                Call.infoMessage(event.player.con, Const.WELCOME_EN);
            }
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            Log.info(event.player.name + " has disconnected from the server");
            Call.sendMessage("[scarlet]- [accent]" + event.player.name + "[scarlet] вышел");
        });

        // блок "торийки"
        Events.on(EventType.DepositEvent.class, event -> {
            Building building = event.tile;
            Player player = event.player;
            if (building.block() == Blocks.thoriumReactor && event.item == Items.thorium && player.team().cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                Groups.player.each(p -> p.sendMessage(MessageFormat.format("[scarlet]ВНИМАНИЕ! [accent]{0} положил торий в реактор!\n x: [lightgray]{1}[accent], y: [lightgray]{2}", player, building.tileX(), building.tileY())));
            }
        });

        Events.on(EventType.BuildSelectEvent.class, event -> {
            if (!event.breaking && event.builder != null && event.builder.buildPlan() != null &&
                    event.builder.buildPlan().block == Blocks.thoriumReactor && event.builder.isPlayer() &&
                    event.team.cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                Player player = event.builder.getPlayer();
                if (interval.get(300)) {
                    Groups.player.each(p -> p.sendMessage(MessageFormat.format("[scarlet]ВНИМАНИЕ! [accent]{0} строит ториевый реактор возле ядра!\nx: [lightgray]{1}[accent], y: [lightgray]{2}", player.name, event.tile.x, event.tile.y)));
                }
            }
        });
        // конец блока

        // блок "чат"
        Events.on(EventType.PlayerChatEvent.class, event -> {
            String prefix = event.player.admin() ? "\uE82C" : "\uE872";
            Groups.player.each(player -> {
                String translated = event.message;
                if (!event.message.startsWith("/")) {
                    try {
                        if (!player.locale.equals(event.player.locale())) {
                            translated = Translator.translate(event.message, player.locale, "auto");
                        }
                    } catch (IOException e) {
                        Log.err(e.getMessage());
                    } finally {
                        String msg = MessageFormat.format(Const.CHAT_FORMAT, prefix, event.player.name, translated);
                        player.sendMessage(msg);
                    }
                }
            });
        });
        // конец блока

        // блок "история"
        Events.on(EventType.WorldLoadEvent.class, event -> {
            history = new CacheSeq[world.width()][world.height()];
            for (Tile tile : world.tiles) {
                history[tile.x][tile.y] = Seqs.newBuilder()
                        .maximumSize(15)
                        .expireAfterWrite(Duration.ofMillis(1800000))
                        .build();
            }
        });

        netServer.admins.addActionFilter(action -> {
            if (action.type == Administration.ActionType.rotate) {
                HistoryEntry entry = new RotateEntry(action.player, action.tile.build.block, action.rotation);
                history[action.tile.x][action.tile.y].add(entry);
            }
            return true;
        });

        Events.on(EventType.BlockBuildEndEvent.class, event -> {
            HistoryEntry historyEntry = new BlockEntry(event);
            Seq<Tile> linkedTile = event.tile.getLinkedTiles(new Seq<>());
            for (Tile tile : linkedTile) {
                history[tile.x][tile.y].add(historyEntry);
            }
        });

        Events.on(EventType.ConfigEvent.class, event -> {
            if (event.tile.block instanceof LogicBlock || event.player == null || event.tile.tileX() > world.width() || event.tile.tileY() > world.height()) {
                return;
            }

            CacheSeq<HistoryEntry> entries = history[event.tile.tileX()][event.tile.tileY()];
            boolean connect = true;

            HistoryEntry last = entries.peek();
            if (!entries.isEmpty() && last instanceof ConfigEntry) {
                ConfigEntry lastConfigEntry = (ConfigEntry) last;

                connect = !event.tile.getPowerConnections(new Seq<>()).isEmpty() &&
                        !(lastConfigEntry.value instanceof Integer && event.value instanceof Integer &&
                                (int) lastConfigEntry.value == (int) event.value && lastConfigEntry.connect);
            }

            HistoryEntry entry = new ConfigEntry(event, connect);

            Seq<Tile> linkedTile = event.tile.tile.getLinkedTiles(new Seq<>());
            for (Tile tile : linkedTile) {
                history[tile.x][tile.y].add(entry);
            }
        });
        // конец блока
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.removeCommand("a");

        handler.removeCommand("t");

        handler.<Player>register("a", "<текст...>", "Отправить сообщение администрации", (args, player) -> {
            String message = args[0];
            if (player.admin()) {
                Groups.player.each(otherPlayer -> {
                    if (otherPlayer.admin()) {
                        String translated = message;
                        try {
                            translated = Translator.translate(message, otherPlayer.locale, "auto");
                        } catch (IOException e) {
                            Log.err(e.getMessage());
                        } finally {
                            String prefix = player.admin() ? "\uE82C" : "\uE872";
                            String msg = MessageFormat.format(Const.CHAT_FORMAT, prefix, player.name, translated);
                            otherPlayer.sendMessage("<[scarlet]A[]>" + msg);
                        }
                    }
                });
            } else {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду!");
            }
        });

        handler.<Player>register("t", "<текст...>", "Отправить сообщение команде", (args, player) -> {
            String message = args[0];
            Groups.player.each(otherPlayer -> {
                if (otherPlayer.team() == player.team()) {
                    String translated = message;
                    try {
                        translated = Translator.translate(message, otherPlayer.locale, "auto");
                    } catch (IOException e) {
                        Log.err(e.getMessage());
                    } finally {
                        String prefix = player.admin() ? "\uE82C" : "\uE872";
                        String msg = MessageFormat.format(Const.CHAT_FORMAT, prefix, player.name, translated);
                        otherPlayer.sendMessage("<[#" + player.team().color.toString().substring(0, 6) + "]T[]>" + msg);
                    }
                }
            });
        });

        handler.<Player>register("rules", "Посмотреть список правил", (args, player) -> {
            if (player.locale.startsWith("uk")) {
                player.sendMessage(Const.RULES_UK);
            } else if (player.locale.startsWith("ru")) {
                player.sendMessage(Const.RULES_RU);
            } else {
                player.sendMessage(Const.RULES_EN);
            }
        });

        handler.<Player>register("hub", "Подключиться к Хабу", (args, player) -> Call.connect(player.con, "95.217.224.159", 26788));

        handler.<Player>register("discord", "Получить ссылку на Discord сервер", (args, player) -> player.sendMessage("https://discord.gg/RkbFYXFU9E"));

        handler.<Player>register("connect", "[сервер...]", "Подключиться к другому серверу", (args, player) -> {
            if (args.length == 0) {
                player.sendMessage("[sky]Список доступных серверов:\n" + Const.SERVER_LIST);
                return;
            }
            if (args[0].equalsIgnoreCase("list")) {
                player.sendMessage("[sky]Список доступных серверов:\n" + Const.SERVER_LIST);
                return;
            }
            String serverName = args[0] + (args.length == 2 ? args[1] : "");
            if (!Const.SERVER_ADDRESS.containsKey(serverName.toLowerCase())) {
                player.sendMessage("[scarlet]Такого сервера не существует. Доступные сервера:\n" + Const.SERVER_LIST);
                return;
            }
            String address = Const.SERVER_ADDRESS.get(serverName.toLowerCase());
            String ip = address.split(":")[0];
            int port = Integer.parseInt(address.split(":")[1]);
            Vars.net.pingHost(ip, port, host -> Call.connect(player.con, ip, port), e -> player.sendMessage("[scarlet]Сервер оффлайн"));
        });

        handler.<Player>register("h", "<x> <y>", "Посмотреть историю блока", (args, player) -> {
            if (!Strings.canParseInt(args[0]) || !Strings.canParseInt(args[1])) {
                player.sendMessage("Неверный формат координат");
                return;
            }

            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);

            CacheSeq<HistoryEntry> entries = history[x][y];
            entries.cleanUp();

            StringBuilder message = new StringBuilder(MessageFormat.format("[orange]История блока ([lightgray]{0}[gray],[lightgray]{1}[orange])", x, y));
            if (entries.isEmpty()) {
                message.append("\\n[royal]* [lightgray]записи отсутствуют");
            } else if (entries.isOverflown()) {
                message.append("\n[lightgray]... слишком много записей");
            } else {
                for (HistoryEntry entry : entries) {
                    message.append("\n").append(entry.getMessage());
                }
            }

            player.sendMessage(message.toString());
        });

        handler.<Player>register("spawn", "<юнит> [количество] [команда]", "Заспавнить юнитов", (args, player) -> {
            if (!player.admin) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду!");
                return;
            }
            UnitType unit = Vars.content.units().find(b -> b.name.equalsIgnoreCase(args[0]));
            if (unit == null) {
                player.sendMessage("[scarlet]Юнит не найден! Доступные юниты:\n\n" + Const.UNIT_LIST + "\n");
                return;
            }
            int count = (args.length > 1 && Strings.canParseInt(args[1])) ? Strings.parseInt(args[1]) : 1;
            if (count > 24) {
                player.sendMessage("[scarlet]Нельзя заспавнить больше 24 юнитов!");
                return;
            }
            if (count < 1) {
                player.sendMessage("[scarlet]Нельзя заспавнить меньше 1 юнита!");
                return;
            }
            Team team = args.length > 2 ? Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[2])) : player.team();
            if (team == null) {
                player.sendMessage("[scarlet]Неверная команда. Возможные варианты:\n" + Const.TEAM_LIST);
                return;
            }
            for (int i = 0; count > i; i++) {
                unit.spawn(team, player.x, player.y);
            }
            player.sendMessage("[green]Ты заспавнил " + "[accent]" + count + " " + unit + " " + "[green]для команды " + "[#" + team.color.toString().substring(0, 6) + "]" + team);
        });

        handler.<Player>register("team", "<команда>", "Изменить команду", (args, player) -> {
            if (!player.admin()) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду![]");
                return;
            }
            Team team = Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[0]));
            if (team == null) {
                player.sendMessage("[scarlet]Неверная команда. Возможные варианты:\n" + Const.TEAM_LIST);
                return;
            }
            player.team(team);
            player.sendMessage("Команда изменена. Новая команда - [#" + team.color.toString().substring(0, 6) + "]" + team);
        });

        handler.<Player>register("end", "Принудительно сменить карту", (args, player) -> {
            if (!player.admin) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду![]");
            } else {
                Events.fire(new EventType.GameOverEvent(Team.crux));
            }
        });
    }
}
