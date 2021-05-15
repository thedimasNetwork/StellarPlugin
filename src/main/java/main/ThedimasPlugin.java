package main;

import arc.*;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.serialization.Jval;
import database.*;
import history.entry.BlockEntry;
import history.entry.ConfigEntry;
import history.entry.RotateEntry;
import history.struct.Seqs;
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

import history.struct.CacheSeq;
import history.entry.HistoryEntry;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;
import util.Bundle;
import util.Translator;

import java.io.*;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.*;

@SuppressWarnings({"unused", "unchecked"})
public class ThedimasPlugin extends Plugin {

    private boolean autoPause = true;

    private final Interval interval = new Interval();

    private final Set<String> votesRTV = new HashSet<>();

    private CacheSeq<HistoryEntry>[][] history;

    private final Map<String, Boolean> activeHistoryPlayers = new HashMap<>();

    private final Map<String, String> admins = new HashMap<>();

    private CacheSeq<HistoryEntry> getHistorySeq(int x, int y) {
        CacheSeq<HistoryEntry> seq = history[x][y];
        if (seq == null) {
            history[x][y] = seq = Seqs.newBuilder()
                    .maximumSize(15)
                    .expireAfterWrite(Duration.ofMillis(1800000L))
                    .build();
        }
        return seq;
    }

    @Override
    public void init() {
        Log.info("thedimasPlugin launched!");

        Vars.state.serverPaused = true;

        netServer.admins.addChatFilter((player, message) -> null);

        Core.net.httpGet(ghApi + "/search/code?q=name+repo:Anuken/Mindustry+filename:bundle&per_page=100", res -> {
            Jval json = Jval.read(res.getResultAsString());
            Seq<String> names = json.get("items").asArray().map(obj -> obj.getString("name"))
                    .filter(str -> str.endsWith(".properties") && !str.equals("bundle.properties"))
                    .map(str -> str.substring("bundle".length() + 1, str.lastIndexOf('.')))
                    .and("en");

            locales = new Locale[names.size];
            for (int i = 0; i < locales.length; i++) {
                String code = names.get(i);
                if (code.contains("_")) {
                    String[] codes = code.split("_");
                    locales[i] = new Locale(codes[0], codes[1]);
                } else {
                    locales[i] = new Locale(code);
                }
            }

            Arrays.sort(locales, Structs.comparing(l -> l.getDisplayName(l), String.CASE_INSENSITIVE_ORDER));
            locales = Seq.with(locales).and(new Locale("router")).toArray(Locale.class);
        }, Log::err);

        Events.on(EventType.PlayerJoin.class, event -> {
            if (Groups.player.size() >= 1 && autoPause && Vars.state.serverPaused) {
                Vars.state.serverPaused = false;
                Log.info("auto-pause: " + Groups.player.size() + " player(s) connected -> Game unpaused...");
            }

            Log.info(MessageFormat.format(Const.JOIN_LOG_FORMAT, event.player.name, event.player.locale, event.player.con.address));
            String playerName = NetClient.colorizeName(event.player.id, event.player.name);
            bundled("player.onJoin", playerName);

            if (event.player.locale.startsWith("uk")) {
                Call.infoMessage(event.player.con, Const.WELCOME_UK);
            } else if (event.player.locale.startsWith("ru")) {
                Call.infoMessage(event.player.con, Const.WELCOME_RU);
            } else {
                Call.infoMessage(event.player.con, Const.WELCOME_EN);
            }

            try {
                if (!DBHandler.userExist(event.player.uuid())) {
                    PlayerData data = new PlayerData();
                    data.uuid = event.player.uuid();
                    data.ip = event.player.ip();
                    data.name = event.player.name();
                    data.locale = event.player.locale;
                    data.translator = "auto";
                    data.admin = event.player.admin;
                    data.banned = false; //banned

                    DBHandler.save(data);
                } else {
                    DBHandler.update(event.player.uuid(), database.Const.U_NAME, event.player.name);
                    DBHandler.update(event.player.uuid(), database.Const.U_LOCALE, event.player.locale);
                    DBHandler.update(event.player.uuid(), database.Const.U_IP, event.player.ip());
                    String banned = DBHandler.get(event.player.uuid(), database.Const.U_BANNED);
                    if("1".equals(banned)) {
                        netServer.admins.banPlayer(event.player.uuid());
                        netServer.admins.banPlayerIP(event.player.ip());
                    }
                }
                String admin = DBHandler.get(event.player.uuid(), database.Const.U_ADMIN);
                if ("1".equals(admin)) {
                    admins.put(event.player.uuid(), event.player.name);
                    event.player.admin = true;
                }
            } catch (SQLException e) {
                Log.err(e);
            }
        });

        Events.on(EventType.GameOverEvent.class, e -> votesRTV.clear());

        Events.on(EventType.PlayerLeave.class, event -> {
            if (Groups.player.size() - 1 < 1 && autoPause) {
                Vars.state.serverPaused = true;
                Log.info("auto-pause: " + (Groups.player.size() - 1) + " player connected -> Game paused...");
            }

            if(votesRTV.contains(event.player.uuid())) {
                votesRTV.remove(event.player.uuid());
                int cur = votesRTV.size();
                int req = (int) Math.ceil(Const.VOTES_RATIO * Groups.player.size());
                String playerName = NetClient.colorizeName(event.player.id, event.player.name);
                bundled("rtv.leave", playerName, cur, req);
            }

            activeHistoryPlayers.remove(event.player.uuid());

            Log.info(event.player.name + " has disconnected from the server");
            String playerName = NetClient.colorizeName(event.player.id, event.player.name);
            bundled("player.onLeave", playerName);
        });

        // блок "торийки"
        Events.on(EventType.DepositEvent.class, event -> {
            Building building = event.tile;
            Player target = event.player;
            if(building.block() == Blocks.thoriumReactor && event.item == Items.thorium &&
                    target.team().cores().contains(c -> event.tile.dst(c.x, c.y) < 300)){
                String playerName = NetClient.colorizeName(event.player.id, event.player.name);
                Groups.player.each(p -> p.sendMessage(MessageFormat.format("[scarlet]ВНИМАНИЕ! [accent]{0}[accent] положил торий в реактор!\n" +
                        "x: [lightgray]{1}[accent], y: [lightgray]{2}", playerName, building.tileX(), building.tileY())));
                Log.info(MessageFormat.format("{0} положил торий в реактор ({1}, {2})", target.name, building.tileX(), building.tileY()));
            }
        });

        Events.on(EventType.BuildSelectEvent.class, event -> {
            if (!event.breaking && event.builder != null && event.builder.buildPlan() != null &&
                    event.builder.buildPlan().block == Blocks.thoriumReactor && event.builder.isPlayer() &&
                    event.team.cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                Player player = event.builder.getPlayer();
                String playerName = NetClient.colorizeName(player.id, player.name);
                if (interval.get(300)) {
                    Groups.player.each(p -> p.sendMessage(MessageFormat.format("[scarlet]ВНИМАНИЕ! [accent]{0}[accent] строит ториевый реактор возле ядра!\n" +
                            "x: [lightgray]{1}[accent], y: [lightgray]{2}", playerName, event.tile.x, event.tile.y)));
                    Log.info(MessageFormat.format("{0} начал строить ториевый реактор близко к ядру ({1}, {2})", player.name, event.tile.x, event.tile.y));
                }
            }
        });
        // конец блока

        // блок "чат"
        Events.on(EventType.PlayerChatEvent.class, event -> {
            if (!event.message.startsWith("/")) {
                String prefix = event.player.admin() ? "\uE82C" : "\uE872";
                String playerName = NetClient.colorizeName(event.player.id, event.player.name);

                Groups.player.each(otherPlayer -> {
                    String translated = event.message;
                    String locale = otherPlayer.locale;
                    try {
                        locale = DBHandler.get(otherPlayer.uuid(), database.Const.U_TRANSLATOR);
                    } catch (Throwable t) {
                        Log.err(t);
                    }

                    if (!otherPlayer.locale.equals(event.player.locale())) {
                        try {
                            translated = Translator.translate(event.message, "auto".equals(locale) || "double".equals(locale) ? otherPlayer.locale : locale, "auto");
                        } catch (Throwable t) {
                            Log.err(t.getMessage());
                            otherPlayer.sendMessage(MessageFormat.format(Const.CHAT_FORMAT, prefix, playerName, translated));
                        }
                    }
                    otherPlayer.sendMessage(MessageFormat.format("double".equals(locale) ? Const.CHAT_FORMAT_DETAILED : Const.CHAT_FORMAT,
                            prefix, playerName, translated, event.message));
                });

                Log.info(MessageFormat.format(Const.CHAT_LOG_FORMAT, Strings.stripColors(event.player.name),  Strings.stripColors(event.message), event.player.locale));
            }
        });
        // конец блока

        //блок "баны"
        Events.on(EventType.PlayerBanEvent.class, event -> {
            try {
                String ip = DBHandler.get(event.player.uuid(), database.Const.U_IP);
                netServer.admins.banPlayer(event.player.uuid());
                netServer.admins.banPlayerIP(event.player.ip());
                DBHandler.update(event.player.uuid(), database.Const.U_BANNED, "1");
            } catch (SQLException e) {
                Log.err(e.getMessage());
            }
        });

        Events.on(EventType.PlayerIpBanEvent.class, event -> netServer.admins.banPlayerIP(event.ip));

        Events.on(EventType.PlayerUnbanEvent.class, event -> {
            try {
                String ip = DBHandler.get(event.player.uuid(), database.Const.U_IP);
                netServer.admins.unbanPlayerID(event.player.uuid());
                netServer.admins.unbanPlayerIP(event.player.ip());
                DBHandler.update(event.player.uuid(), database.Const.U_BANNED, "0");
            } catch (SQLException e) {
                Log.err(e.getMessage());
            }
        });

        Events.on(EventType.PlayerIpUnbanEvent.class, event -> netServer.admins.unbanPlayerIP(event.ip));
        // конец блока

        // блок "история"
        Events.on(EventType.WorldLoadEvent.class, event -> {
            if (Groups.player.size() > 0 && autoPause) {
                Vars.state.serverPaused = false;
                Log.info("auto-pause: " + Groups.player.size() + " player(s) connected -> Game unpaused...");
            }

            history = new CacheSeq[world.width()][world.height()];
        });

        netServer.admins.addActionFilter(action -> {
            if (action.type == Administration.ActionType.rotate) {
                HistoryEntry entry = new RotateEntry(action.player, action.tile.build.block, action.rotation);
                getHistorySeq(action.tile.x, action.tile.y).add(entry);
            }
            return true;
        });

        Events.on(EventType.BlockBuildEndEvent.class, event -> {
            HistoryEntry historyEntry = new BlockEntry(event);
            Seq<Tile> linkedTile = event.tile.getLinkedTiles(new Seq<>());
            for (Tile tile : linkedTile) {
                getHistorySeq(tile.x, tile.y).add(historyEntry);
            }
        });

        Events.on(EventType.ConfigEvent.class, event -> {
            if (event.tile.block instanceof LogicBlock || event.player == null || event.tile.tileX() > world.width() || event.tile.tileY() > world.height()) {
                return;
            }

            CacheSeq<HistoryEntry> entries = getHistorySeq(event.tile.tileX(), event.tile.tileY());
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
                getHistorySeq(tile.x, tile.y).add(entry);
            }
        });

        Events.on(EventType.TapEvent.class, event -> {
            if (activeHistoryPlayers.containsKey(event.player.uuid())) {
                int x = event.tile.x;
                int y = event.tile.y;

                CacheSeq<HistoryEntry> entries = getHistorySeq(x, y);

                boolean detailed = activeHistoryPlayers.get(event.player.uuid());

                StringBuilder result = new StringBuilder();
                result.append(MessageFormat.format("[orange]-- История Блока ([lightgray]{0}[gray],[lightgray]{1}[orange]) --", x, y)).append("\n");

                entries.cleanUp();
                if (entries.isEmpty()) {
                    result.append("[royal]* [lightgray]записи отсутствуют\n");
                }

                for (int i = 0; i < entries.size && i < Const.HISTORY_PAGE_SIZE; i++) {
                    HistoryEntry entry = entries.get(i);

                    result.append(entry.getMessage());
                    if (detailed) {
                        result.append(" [lightgray]").append(entry.getLastAccessTime(TimeUnit.MINUTES)).append(" минут назад");
                    }

                    result.append("\n");
                }

                event.player.sendMessage(result.toString());
            }
        });
        // конец блока
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("export-players", "Export players into DB", args -> {
            ObjectMap<String, Administration.PlayerInfo> playerList = Reflect.get(netServer.admins, "playerInfo");
            int exported = 0;
            for (Administration.PlayerInfo info : playerList.values()) {
                PlayerData data = new PlayerData();
                data.uuid = info.id;
                data.ip = info.lastIP;
                data.name = info.lastName;
                data.locale = "undefined";
                data.translator = "auto";
                data.admin = info.admin;
                data.banned = info.banned;

                try {
                    if(!DBHandler.userExist(info.id)) {
                        DBHandler.save(data);
                        exported++;
                    }
                } catch (SQLException e) {
                    Log.err(e.getMessage());
                    Log.err(MessageFormat.format("Unable to export data of player {0} ({1})", Strings.stripColors(info.lastName), Strings.stripColors(info.id)));
                }
            }
            Log.info(MessageFormat.format("Successfully exported {0} players", exported));
        });

        handler.register("auto-pause", "[on|off]", "Pause game with 0 people online", args -> {
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
        handler.<Player>register("a", "<текст...>", "Отправить сообщение администрации", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду!");
                Log.info(MessageFormat.format("{0} попытался отправить сообщение админам", Strings.stripColors(player.name)));
                return;
            }

            String message = args[0];
            String prefix = "\uE82C";
            String playerName = NetClient.colorizeName(player.id, player.name);

            Groups.player.each(Player::admin, otherPlayer -> {
                String translated = message;
                String locale = otherPlayer.locale;
                try {
                    locale = DBHandler.get(otherPlayer.uuid(), database.Const.U_TRANSLATOR);
                } catch (Throwable t) {
                    Log.err(t);
                }

                if (!otherPlayer.locale.equals(player.locale())) {
                    try {
                        translated = Translator.translate(message, "auto".equals(locale) || "double".equals(locale) ? otherPlayer.locale : locale, "auto");
                    } catch (IOException e) {
                        Log.err(e.getMessage());

                        String msg = MessageFormat.format("double".equals(locale) ? Const.CHAT_FORMAT_DETAILED : Const.CHAT_FORMAT, prefix, playerName, translated, message);
                        otherPlayer.sendMessage("<[scarlet]A[]>" + msg);
                    }
                }
                String msg = MessageFormat.format("double".equals(locale) ? Const.CHAT_FORMAT_DETAILED : Const.CHAT_FORMAT, prefix, playerName, translated, message);
                otherPlayer.sendMessage("<[scarlet]A[]>" + msg);
            });

            Log.info("<A>" + MessageFormat.format(Const.CHAT_LOG_FORMAT, Strings.stripColors(player.name),  Strings.stripColors(message), player.locale));
        });

        handler.removeCommand("t");
        handler.<Player>register("t", "<текст...>", "Отправить сообщение команде", (args, player) -> {
            String message = args[0];
            String playerName = NetClient.colorizeName(player.id, player.name);
            String prefix = player.admin() ? "\uE82C" : "\uE872";

            Groups.player.each(o -> o.team() == player.team(), otherPlayer -> {
                String translated = message;
                String locale = otherPlayer.locale;
                try {
                    locale = DBHandler.get(otherPlayer.uuid(), database.Const.U_TRANSLATOR);
                } catch (Throwable t) {
                    Log.err(t);
                }

                if (!otherPlayer.locale.equals(player.locale())) {
                    try {
                        translated = Translator.translate(message, "auto".equals(locale) || "double".equals(locale) ? otherPlayer.locale : locale, "auto");
                    } catch (Throwable t) {
                        Log.err(t.getMessage());

                        String msg = MessageFormat.format("double".equals(locale) ? Const.CHAT_FORMAT_DETAILED : Const.CHAT_FORMAT, prefix, playerName, translated, message);
                        otherPlayer.sendMessage("<[#" + player.team().color + "]T[]>" + msg);
                    }
                }
                String msg = MessageFormat.format("double".equals(locale) ? Const.CHAT_FORMAT_DETAILED : Const.CHAT_FORMAT, prefix, playerName, translated, message);
                otherPlayer.sendMessage("<[#" + player.team().color + "]T[]>" + msg);
            });

            Log.info("<T>" + MessageFormat.format(Const.CHAT_LOG_FORMAT, Strings.stripColors(player.name), Strings.stripColors(message), player.locale));
        });

        handler.removeCommand("help");
        handler.<Player>register("help", "[page]", "Посмотреть список доступных команд", (args, player) -> {
            if(args.length > 0 && !Strings.canParseInt(args[0])){
                bundled(player, "commands.page-not-int");
                return;
            }
            Locale locale = findLocale(player.locale);
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil(handler.getCommandList().size / Const.HISTORY_PAGE_SIZE);

            if (--page >= pages || page < 0) {
                bundled(player, "commands.under-page", pages);
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Bundle.format("commands.help.page", locale, page + 1, pages)).append("\n\n");

            for (int i = 6 * page; i < Math.min(6 * (page + 1), handler.getCommandList().size); i++) {
                CommandHandler.Command command = handler.getCommandList().get(i);
                result.append("[orange] /").append(command.text).append("[white] ")
                        .append(command.paramText)
                        .append("[lightgray] - ")
                        .append(Bundle.has(command.description, locale) ? Bundle.get(command.description, locale) : command.description)
                        .append("\n");
            }
            player.sendMessage(result.toString());
        });

        handler.<Player>register("rtv", "rtv.description", (arg, player) -> {
            votesRTV.add(player.uuid());
            int cur = votesRTV.size();
            int req = (int) Math.ceil(Const.VOTES_RATIO * Groups.player.size());

            String playerName = NetClient.colorizeName(player.id, player.name);
            bundled("rtv.vote", playerName, cur, req);

            if (cur >= req) {
                votesRTV.clear();
                bundled("rtv.passed");
                Events.fire(new EventType.GameOverEvent(Team.crux));
            }
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
            String serverName = args[0].toLowerCase();
            if (!Const.SERVER_ADDRESS.containsKey(serverName)) {
                player.sendMessage("[scarlet]Такого сервера не существует. Доступные сервера:\n" + Const.SERVER_LIST);
                return;
            }
            String[] address = Const.SERVER_ADDRESS.get(serverName).split(":");
            String ip = address[0];
            int port = Integer.parseInt(address[1]);
            Vars.net.pingHost(ip, port, host -> Call.connect(player.con, ip, port), e -> player.sendMessage("[scarlet]Сервер оффлайн"));
        });

        handler.<Player>register("tr", "[off|auto|double|somelocale]", "Настроить переводчик чата.", (args, player) -> {
            String locale;
            try {
                locale = DBHandler.get(player.uuid(), database.Const.U_TRANSLATOR);
            } catch (Throwable t) {
                player.sendMessage("[scarlet]Не получилось получить настройки языка.");
                Log.err(t);
                return;
            }

            if (args.length == 0) {
                player.sendMessage("[sky]Текущий язык переводчика: " + locale);
                return;
            }

            String mode = args[0].toLowerCase();
            switch (mode) {
                case "off":
                    try {
                        DBHandler.update(player.uuid(), database.Const.U_TRANSLATOR, "off");
                    } catch (Throwable t) {
                        Log.err(t);
                    }

                    player.sendMessage("[sky]Перевод чата выключен.");
                    break;
                case "auto":
                    try {
                        DBHandler.update(player.uuid(), database.Const.U_TRANSLATOR, "auto");
                    } catch (Throwable t) {
                        Log.err(t);
                    }

                    player.sendMessage("[sky]Перевод чата установлен в автоматический режим.");
                    break;
                case "double":
                    try {
                        DBHandler.update(player.uuid(), database.Const.U_TRANSLATOR, "double");
                    } catch (Throwable t) {
                        Log.err(t);
                    }

                    player.sendMessage("[sky]Перевод чата установлен в автоматический режим c отображением оригинального сообщения.");
                    break;
                default:
                    Locale target = Structs.find(locales, l -> mode.equalsIgnoreCase(l.toString()));
                    if (target == null) {
                        player.sendMessage("[sky]Список доступных локализаций:\n" + Const.LocaleListHolder.LOCALE_LIST);
                        return;
                    }

                    try {
                        DBHandler.update(player.uuid(), database.Const.U_TRANSLATOR, target.toString());
                    } catch (Throwable t) {
                        Log.err(t);
                    }

                    player.sendMessage("[sky]Язык переводчика установлен на: " + target);
                    break;
            }
        });

        handler.<Player>register("history", "[страница] [подробно]", "Посмотреть историю блока", (args, player) -> {
            boolean detailed = args.length == 2 && Structs.contains(Const.BOOL_VALUES.split(", "), args[1].toLowerCase());

            if (args.length > 0 && activeHistoryPlayers.containsKey(player.uuid())) {
                if (!Strings.canParseInt(args[0])) {
                    bundled(player, "commands.page-not-int");
                    return;
                }

                int mouseX = Mathf.clamp(Mathf.round(player.mouseX / 8), 1, world.width());
                int mouseY = Mathf.clamp(Mathf.round(player.mouseY / 8), 1, world.height());

                CacheSeq<HistoryEntry> entries = getHistorySeq(mouseX, mouseY);

                int page = Integer.parseInt(args[0]) - 1;
                int pages = Mathf.ceil(entries.size / Const.HISTORY_PAGE_SIZE);

                if (page >= pages || pages < 0 || page < 0) {
                    bundled(player, "commands.under-page", page);
                    return;
                }

                StringBuilder result = new StringBuilder();
                result.append(MessageFormat.format("[orange]-- История Блока ([lightgray]{0}[gray],[lightgray]{1}[orange]) Страница [lightgray]{2}[gray]/[lightgray]{3}[orange] --", mouseX, mouseY, page + 1, pages)).append("\n");

                if (entries.isEmpty()) {
                    result.append("[royal]* [lightgray]записи отсутствуют\n");
                }

                for (int i = 6 * page; i < Math.min(6 * (page + 1), entries.size); i++) {
                    HistoryEntry entry = entries.get(i);
                    result.append(entry.getMessage());
                    if (detailed) {
                        result.append(" [lightgray]").append(entry.getLastAccessTime(TimeUnit.MINUTES)).append(" минут назад");
                    }
                    result.append("\n");
                }

                player.sendMessage(result.toString());
            } else if (activeHistoryPlayers.containsKey(player.uuid())) {
                activeHistoryPlayers.remove(player.uuid());
                player.sendMessage("[lightgray]История [orange]выключена");
            } else if (args.length == 2) {
                activeHistoryPlayers.put(player.uuid(), detailed);
                String msg = detailed ? "[lightgray]Подробная история" : "[lightgray]История";
                player.sendMessage(msg + " [orange]включена[]. Нажмите на тайл для просмотра информации");
            } else {
                activeHistoryPlayers.put(player.uuid(), false);
                player.sendMessage("[lightgray]История [orange]включена[]. Нажмите на тайл для просмотра информации");
            }
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
                player.name(admins.get(player.uuid()));
                String playerName = NetClient.colorizeName(player.id, player.name);
                player.sendMessage("[green]Ваше имя сброшено. Текущее имя - []" + playerName);
            } else {
                player.name(args[0]);
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

            int count = args.length > 1 && Strings.canParseInt(args[1]) ? Strings.parseInt(args[1]) : 1;
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
            player.sendMessage("[green]Ты заспавнил [accent]" + count + " " + unit + " [green]для команды [#" + team.color.toString().substring(0, 6) + "]" + team);
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

        handler.<Player>register("tp", "<x> <y> [name...]", "Телепортировать игрока по координатам", (args, player) -> {
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

            if (args.length == 3) {
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

            player.sendMessage("Вы телепортированы на координаты [accent]" + x + "[], [accent]" + y);
        });

        handler.<Player>register("kill", "[username...]", "Убить игрока", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду![]");
                return;
            }

            if (args.length == 0) {
                player.unit().kill();
                Log.info(MessageFormat.format("{0} убил сам себя", Strings.stripColors(player.name)));
            } else {
                Player otherPlayer = Groups.player.find(p -> Strings.stripGlyphs(Strings.stripColors(p.name())).equalsIgnoreCase(args[0]));
                if (otherPlayer != null) {
                    otherPlayer.unit().kill();

                    String otherPlayerName = NetClient.colorizeName(otherPlayer.id, otherPlayer.name);
                    player.sendMessage("[accent]Вы успешно убили игрока " + otherPlayerName +
                            "\n[orange]Дьявол доволен Вами. =)");
                    Log.info(MessageFormat.format("{0} убил {1}", Strings.stripColors(player.name), Strings.stripColors(otherPlayerName)));
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
            Log.info(MessageFormat.format("{0} поставил игру на паузу", Strings.stripColors(player.name)));
        });

        handler.<Player>register("end", "Принудительно сменить карту", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду![]");
            } else {
                Events.fire(new EventType.GameOverEvent(Team.crux));
                Log.info(MessageFormat.format("{0} сменил карту принудительно", Strings.stripColors(player.name)));
            }
        });

        handler.<Player>register("killall", "Убить ВСЕХ", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду![]");
                return;
            }
            Groups.unit.each(Unitc::kill);
            player.sendMessage("[scarlet]Вы убили всех");
            Log.info(MessageFormat.format("{0} убил всех", Strings.stripColors(player.name)));
        });
        // конец блока
    }

    public static void bundled(Player player, String key, Object... values) {
        player.sendMessage(Bundle.format(key, findLocale(player.locale), values));
    }

    public static void bundled(String key, Object... values) {
        Groups.player.each(p -> bundled(p, key, values));
    }

    private static Locale findLocale(String code) {
        Locale locale = Structs.find(Const.supportedLocales, l -> l.toString().equals(code));
        return locale != null ? locale : Const.defaultLocale();
    }
}
