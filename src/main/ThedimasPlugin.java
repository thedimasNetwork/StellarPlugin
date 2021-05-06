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
import mindustry.core.NetClient;
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
import java.util.HashMap;
import java.util.Map;

import static mindustry.Vars.*;

@SuppressWarnings({"unused", "unchecked"})
public class ThedimasPlugin extends Plugin {

    private boolean autoPause = true;

    private final Interval interval = new Interval();

    private CacheSeq<HistoryEntry>[][] history;

    private final Map<String, String> admins = new HashMap<>();

    //called when game initializes
    @Override
    public void init() {
        Log.info("thedimasPlugin launched!");

        Vars.state.serverPaused = true;

        netServer.admins.addChatFilter((player, message) -> null);

        Events.on(EventType.PlayerJoin.class, event -> {
            if (Groups.player.size() >= 1 && autoPause && Vars.state.serverPaused) {
                Vars.state.serverPaused = false;
                Log.info("auto-pause: " + Groups.player.size() + " player(s) connected -> Game unpaused...");
            }

            if (event.player.admin) {
                admins.put(event.player.uuid(), event.player.name);
            }

            String playerName = NetClient.colorizeName(event.player.id, event.player.name);
            Log.info(MessageFormat.format(Const.JOIN_LOG_FORMAT, playerName, event.player.locale, event.player.con.address));
            Call.sendMessage("[lime]+ [accent]" + playerName + "[lime] присоединился");

            if (event.player.locale.startsWith("uk")) {
                Call.infoMessage(event.player.con, Const.WELCOME_UK);
            } else if (event.player.locale.startsWith("ru")) {
                Call.infoMessage(event.player.con, Const.WELCOME_RU);
            } else {
                Call.infoMessage(event.player.con, Const.WELCOME_EN);
            }
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            if (Groups.player.size() - 1 < 1 && autoPause) {
                Vars.state.serverPaused = true;
                Log.info("auto-pause: " + (Groups.player.size() - 1) + " player connected -> Game paused...");
            }

            if (admins.containsKey(event.player.uuid())) {
                event.player.admin = true;
            }

            String playerName = NetClient.colorizeName(event.player.id, event.player.name);
            Log.info(playerName + " has disconnected from the server");
            Call.sendMessage("[scarlet]- [accent]" + playerName + "[scarlet] вышел");
        });

        // блок "торийки"
        Events.on(EventType.DepositEvent.class, event -> {
            Building building = event.tile;
            Player player = event.player;
            if (building.block() == Blocks.thoriumReactor && event.item == Items.thorium && player.team().cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                Groups.player.each(p -> p.sendMessage(MessageFormat.format("[scarlet]ВНИМАНИЕ! [accent]{0} положил торий в реактор!\n x: [lightgray]{1}[accent], y: [lightgray]{2}", player, building.tileX(), building.tileY())));
                Log.info(MessageFormat.format("{0} положил торий в реактор ({1}, {2})", player, building.tileX(), building.tileY()));
            }
        });

        Events.on(EventType.BuildSelectEvent.class, event -> {
            if (!event.breaking && event.builder != null && event.builder.buildPlan() != null &&
                    event.builder.buildPlan().block == Blocks.thoriumReactor && event.builder.isPlayer() &&
                    event.team.cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                Player player = event.builder.getPlayer();
                String playerName = NetClient.colorizeName(player.id, player.name);
                if (interval.get(300)) {
                    Groups.player.each(p -> p.sendMessage(MessageFormat.format("[scarlet]ВНИМАНИЕ! [accent]{0} строит ториевый реактор возле ядра!\nx: [lightgray]{1}[accent], y: [lightgray]{2}", playerName, event.tile.x, event.tile.y)));
                    Log.info(MessageFormat.format("{0} начал строить ториевый реактор близко к ядру ({1}, {2})", playerName, event.tile.x, event.tile.y));
                }
            }
        });
        // конец блока

        // блок "чат"
        Events.on(EventType.PlayerChatEvent.class, event -> {
            if (!event.message.startsWith("/")) {
                String prefix = event.player.admin() ? "\uE82C" : "\uE872";
                Groups.player.each(player -> {
                    String translated = event.message;
                    try {
                        if (!player.locale.equals(event.player.locale())) {
                            translated = Translator.translate(event.message, player.locale, "auto");
                        }
                    } catch (IOException e) {
                        Log.err(e.getMessage());
                    } finally {
                        String playerName = NetClient.colorizeName(event.player.id, event.player.name);
                        String msg = MessageFormat.format(Const.CHAT_FORMAT, prefix, playerName, translated);
                        player.sendMessage(msg);
                    }
                });
                Log.info(MessageFormat.format(Const.CHAT_LOG_FORMAT, Strings.stripColors(event.player.name),  Strings.stripColors(event.message), event.player.locale));
            }
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
    public void registerServerCommands(CommandHandler handler) {
        handler.register("auto-pause", "[on|off]", "Поставить игру на паузу, когда никого нет", args -> {
            if (args.length == 0) {
                if (autoPause) {
                    Log.info("Авто-пауза включена");
                } else {
                    Log.info("Авто-пауза выключена");
                }
            }
            if (args[0].equalsIgnoreCase("off")) {
                autoPause = false;
                Log.info("Авто-пауза выключена");

                Vars.state.serverPaused = false;
                Log.info("auto-pause: " + Groups.player.size() + " игрок(ов) онлайн -> Игра снята с паузы...");
            } else if (args[0].equalsIgnoreCase("on")) {
                autoPause = true;
                Log.info("Авто-пауза включена");

                if (Groups.player.size() < 1 && autoPause) {
                    Vars.state.serverPaused = true;
                    Log.info("auto-pause: " + Groups.player.size() + " игроков онлайн -> Игра поставлена на паузу...");
                }
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.removeCommand("a");

        handler.removeCommand("t");

        handler.<Player>register("a", "<текст...>", "Отправить сообщение администрации", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду!");
                Log.info(MessageFormat.format("{0} попытался отправить сообщение админам не будучи админом", Strings.stripColors(player.name)));
                return;
            }

            String message = args[0];
            String prefix = "\uE82C";
            String playerName = NetClient.colorizeName(player.id, player.name);
            Log.info("<A>" + MessageFormat.format(Const.CHAT_LOG_FORMAT, Strings.stripColors(player.name),  Strings.stripColors(message), player.locale));
            Groups.player.each(otherPlayer -> {
                if (otherPlayer.admin()) {
                    String translated = message;
                    try {
                        translated = Translator.translate(message, otherPlayer.locale, "auto");
                    } catch (IOException e) {
                        Log.err(e.getMessage());
                    } finally {
                        String msg = MessageFormat.format(Const.CHAT_FORMAT, prefix, playerName, translated);
                        otherPlayer.sendMessage("<[scarlet]A[]>" + msg);
                    }
                }
            });
        });

        handler.<Player>register("t", "<текст...>", "Отправить сообщение команде", (args, player) -> {
            String message = args[0];
            String playerName = NetClient.colorizeName(player.id, player.name);
            String prefix = player.admin() ? "\uE82C" : "\uE872";
            Log.info("<T>" + MessageFormat.format(Const.CHAT_LOG_FORMAT, Strings.stripColors(player.name),  Strings.stripColors(message), player.locale));
            Groups.player.each(otherPlayer -> {
                if (otherPlayer.team() == player.team()) {
                    String translated = message;
                    try {
                        translated = Translator.translate(message, otherPlayer.locale, "auto");
                    } catch (IOException e) {
                        Log.err(e.getMessage());
                    } finally {
                        String msg = MessageFormat.format(Const.CHAT_FORMAT, prefix, playerName, translated);
                        otherPlayer.sendMessage("<[#" + player.team().color + "]T[]>" + msg);
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

        handler.<Player>register("hub", "Подключиться к Хабу", (args, player) -> {
            String[] address = Const.SERVER_ADDRESS.get("hub").split(":");
            String ip = address[0];
            int port = Integer.parseInt(address[1]);

            Call.connect(player.con, ip, port);
        });

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
            String serverName = args[0];
            if (!Const.SERVER_ADDRESS.containsKey(serverName.toLowerCase())) {
                player.sendMessage("[scarlet]Такого сервера не существует. Доступные сервера:\n" + Const.SERVER_LIST);
                return;
            }
            String[] address = Const.SERVER_ADDRESS.get(serverName.toLowerCase()).split(":");
            String ip = address[0];
            int port = Integer.parseInt(address[1]);
            Vars.net.pingHost(ip, port, host -> Call.connect(player.con, ip, port), e -> player.sendMessage("[scarlet]Сервер оффлайн"));
        });

        handler.<Player>register("history", "<x> <y>", "Посмотреть историю блока", (args, player) -> {
            if (!Strings.canParseInt(args[0]) || !Strings.canParseInt(args[1])) {
                player.sendMessage("[scarlet]Неверный формат координат");
                return;
            }

            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);

            CacheSeq<HistoryEntry> entries = history[x][y];
            entries.cleanUp();

            StringBuilder message = new StringBuilder(MessageFormat.format("[orange]История блока ([lightgray]{0}[gray],[lightgray]{1}[orange])", x, y));
            if (entries.isEmpty()) {
                message.append("\n[royal]* [lightgray]записи отсутствуют");
            } else if (entries.isOverflown()) {
                message.append("\n[lightgray]... слишком много записей");
            } else {
                for (HistoryEntry entry : entries) {
                    message.append("\n").append(entry.getMessage());
                }
            }

            player.sendMessage(message.toString());
        });

        // блок "для админов"
        handler.<Player>register("admin", "Изменить свой статус", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду!");
            } else {
                player.admin = !player.admin;
            }
        });

        handler.<Player>register("name", "[name...]","Изменить свое имя", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду!");
                return;
            }

            if (args.length == 0) {
                player.name = admins.get(player.uuid());
                String playerName = NetClient.colorizeName(player.id, player.name);
                player.sendMessage("[green]Ваше имя сброшено. Текущее имя - []" + playerName);
            } else {
                player.name = args[1];
                String playerName = NetClient.colorizeName(player.id, player.name);
                player.sendMessage("[green]Ваше имя изменено. Текущее имя - []" + playerName);
            }
        });

        handler.<Player>register("spawn", "<юнит> [количество] [команда]", "Заспавнить юнитов", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
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

        handler.<Player>register("team", "<команда> [username...]", "Изменить команду", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду![]");
                return;
            }
            Team team = Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[0]));
            if (team == null) {
                player.sendMessage("[scarlet]Неверная команда. Возможные варианты:\n" + Const.TEAM_LIST);
                return;
            }
            if (args.length == 1) {
                player.team(team);
                player.sendMessage("Ваша команда изменена. Новая команда - [#" + team.color + "]" + team);
            } else {
                Player otherPlayer = Groups.player.find(p -> Strings.stripGlyphs(Strings.stripColors(p.name())).equalsIgnoreCase(args[1]));
                if (otherPlayer != null) {
                    otherPlayer.team(team);
                    otherPlayer.sendMessage("Вашу команду изменили на [#" + team.color + "]" + team);

                    String otherPlayerName = NetClient.colorizeName(otherPlayer.id, otherPlayer.name);
                    player.sendMessage("Вы изменили команду игрока " + otherPlayerName + " []на [#" + team.color + "]" + team);
                } else {
                    player.sendMessage("[scarlet]Игрока с таким ником нет на сервере");
                }
            }
        });

        handler.<Player>register("tp", "<x> <y> [name...]", "Телепортироваться по координатам/к игроку", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду![]");
                return;
            }

            if (!Strings.canParseInt(args[0]) || !Strings.canParseInt(args[1])) {
                player.sendMessage("[scarlet]Неверный формат координат!");
                return;
            }

            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);

            if (x > world.width() || x < 0 || y > world.height() || y < 0) {
                player.sendMessage("[scarlet]Неверные координаты. Максимум: [orange]" + world.width() + "[], [orange]" + world.height() + "[]. Минимум : [orange]0[], [orange]0[].");
                return;
            }

            if (args.length == 2) {
                Player otherPlayer = Groups.player.find(p -> Strings.stripGlyphs(Strings.stripColors(p.name())).equalsIgnoreCase(args[0]));
                if (otherPlayer == null) {
                    player.sendMessage("[scarlet]Игрока с таким ником нет на сервере");
                } else {
                    otherPlayer.unit().set(x * 8, y * 8);
                    Call.setPosition(otherPlayer.con, x * 8, y * 8);
                    otherPlayer.snapSync();

                    String playerName = NetClient.colorizeName(player.id, player.name);
                    otherPlayer.sendMessage("Вас телепортировали на координаты [accent]" + x + "[], [accent]" + y);
                    player.sendMessage("Вы телепортировали " + playerName + " []на координаты [accent]" + x + "[], [accent]" + y);
                }
            }

            player.unit().set(x * 8, y * 8);
            Call.setPosition(player.con, x * 8, y * 8);
            player.snapSync();

            player.sendMessage("Вы телепортированы на координаты [accent]" + x + "[], [accent]" + y);
        });

        handler.<Player>register("kill", "[username...]", "Убить игрока", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду![]");
                return;
            }

            if (args.length == 0) {
                player.unit().kill();
            } else {
                Player otherPlayer = Groups.player.find(p -> Strings.stripGlyphs(Strings.stripColors(p.name())).equalsIgnoreCase(args[0]));
                if (otherPlayer != null) {
                    otherPlayer.unit().kill();

                    String otherPlayerName = NetClient.colorizeName(otherPlayer.id, otherPlayer.name);
                    player.sendMessage("[accent]Вы успешно убили игрока " + otherPlayerName +
                            "\n[orange]Дьявол доволен Вами. =)");
                } else {
                    player.sendMessage("[scarlet]Игрока с таким ником нет на сервере");
                }
            }
        });

        handler.<Player>register("pause", "Поставить игру на паузу", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду![]");
                return;
            }
            Vars.state.serverPaused = !Vars.state.serverPaused;
        });

        handler.<Player>register("end", "Принудительно сменить карту", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду![]");
            } else {
                Events.fire(new EventType.GameOverEvent(Team.crux));
            }
        });
        // конец блока
    }
}
