package stellar;

import arc.*;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import arc.util.Timer;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.content.Items;
import mindustry.core.NetClient;
import mindustry.entities.bullet.BulletType;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.Administration;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;

import stellar.database.*;
import stellar.history.entry.BlockEntry;
import stellar.history.entry.ConfigEntry;
import stellar.history.entry.HistoryEntry;
import stellar.history.entry.RotateEntry;
import stellar.history.struct.CacheSeq;
import stellar.history.struct.Seqs;
import stellar.util.Bundle;
import stellar.util.Translator;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.*;

@SuppressWarnings({"unused", "unchecked"})
public class ThedimasPlugin extends Plugin {

    private boolean autoPause = true;

    private final Interval interval = new Interval(2);

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

        state.serverPaused = true;

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

        Timer.schedule(() -> Groups.build.each(b -> b.block == Blocks.launchPad, building -> {
            if (building.items.total() == 100 && building.power.status > 0.95) {
                building.items.each((item, amount) -> {
                    // TODO: сделать проверку, влезает ли предмет в ядро
                    state.teams.cores(building.team).first().items.add(item, amount);
                });

                Call.clearItems(building);

                float thisX = building.x;
                float thisY = building.y;

                // TODO: искать ближайшее ядро
                float coreX = state.teams.cores(building.team).first().x;
                float coreY = state.teams.cores(building.team).first().y;

                float a = Math.abs(thisX - coreX);
                float b = Math.abs(thisY - coreY);
                float c = (float) Math.hypot(a, b);

                float angle = (float) Math.atan((thisY - coreY) / (thisX - coreX));

                BulletType bullet = Bullets.artilleryDense;
                float baseSpeed = bullet.speed;
                float baseLifetime = bullet.lifetime;

                Call.createBullet(bullet, building.team, thisX, thisY, angle, 0F, 1F, c / baseSpeed / baseLifetime);
            }
        }), 0, 0.1F);

        Events.run(EventType.Trigger.update, () -> {
            if(interval.get(1, 3600)){ // 1 минута
                for (Player p : Groups.player) {
                    try {
                        Long time = Objects.requireNonNull(DBHandler.get(player.uuid(), Table.PLAY_TIME));
                        DBHandler.update(p.uuid(), Table.PLAY_TIME, time + 60);
                    } catch (Throwable t) {
                        Log.err(t);
                    }
                }
            }
        });

        Events.on(EventType.PlayEvent.class, event -> state.rules.revealedBlocks.add(Blocks.launchPad));

        Events.on(EventType.PlayerJoin.class, event -> {
            if (Groups.player.size() >= 1 && autoPause && state.serverPaused) {
                state.serverPaused = false;
                Log.info("auto-pause: " + Groups.player.size() + " player(s) connected -> Game unpaused...");
            }

            Log.info(MessageFormat.format(Const.JOIN_LOG_FORMAT, event.player.name, event.player.locale, event.player.con.address));
            String playerName = NetClient.colorizeName(event.player.id, event.player.name);
            bundled("events.join.player-join", playerName);

            if (event.player.locale.startsWith("uk")) {
                Call.infoMessage(event.player.con, Const.WELCOME_UK);
            } else if (event.player.locale.startsWith("ru")) {
                Call.infoMessage(event.player.con, Const.WELCOME_RU);
            } else {
                Call.infoMessage(event.player.con, Const.WELCOME_EN);
            }

            try {
                if (!DBHandler.userExist(event.player.uuid())) {
                    DBHandler.update(event.player.uuid(), Table.NAME, event.player.name);
                    DBHandler.update(event.player.uuid(), Table.LOCALE, event.player.locale);
                    DBHandler.update(event.player.uuid(), Table.IP, event.player.ip());

                    Boolean banned = DBHandler.get(event.player.uuid(), Table.BANNED);
                    if(banned != null && banned) {
                        netServer.admins.banPlayer(event.player.uuid());
                        netServer.admins.banPlayerIP(event.player.ip());
                    } else {
                        Boolean admin = DBHandler.get(event.player.uuid(), Table.ADMIN);
                        if (admin != null && admin) {
                            admins.put(event.player.uuid(), event.player.name);
                            event.player.admin = true;
                        }
                    }
                } else {
                    PlayerData data = new PlayerData();
                    data.uuid = event.player.uuid();
                    data.ip = event.player.ip();
                    data.name = event.player.name();
                    data.locale = event.player.locale;
                    data.admin = event.player.admin;

                    DBHandler.save(data);
                }
            } catch (SQLException e) {
                Log.err(e);
            }
        });

        Events.on(EventType.ServerLoadEvent.class, event -> Log.info("thedimasPlugin: Server loaded"));

        Events.on(EventType.GameOverEvent.class, e -> votesRTV.clear());

        Events.on(EventType.PlayerLeave.class, event -> {
            if (Groups.player.size() - 1 < 1 && autoPause) {
                state.serverPaused = true;
                Log.info("auto-pause: " + (Groups.player.size() - 1) + " player connected -> Game paused...");
            }

            if(votesRTV.contains(event.player.uuid())) {
                votesRTV.remove(event.player.uuid());
                int cur = votesRTV.size();
                int req = (int) Math.ceil(Const.VOTES_RATIO * Groups.player.size());
                String playerName = NetClient.colorizeName(event.player.id, event.player.name);
                bundled("commands.rtv.leave", playerName, cur, req);
            }

            activeHistoryPlayers.remove(event.player.uuid());

            Log.info(event.player.name + " has disconnected from the server");
            String playerName = NetClient.colorizeName(event.player.id, event.player.name);
            bundled("events.leave.player-leave", playerName);
        });

        // блок "торийки"
        Events.on(EventType.DepositEvent.class, event -> {
            Player target = event.player;
            Building building = event.tile;

            if(building.block() == Blocks.thoriumReactor && event.item == Items.thorium &&
                    target.team().cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                String playerName = NetClient.colorizeName(event.player.id, event.player.name);
                bundled("events.deposit.thorium-in-reactor", playerName, building.tileX(), building.tileY());
                Log.info(MessageFormat.format("{0} положил торий в реактор ({1}, {2})", target.name, building.tileX(), building.tileY()));
            }
        });

        Events.on(EventType.BuildSelectEvent.class, event -> {
            if (!event.breaking && event.builder != null && event.builder.buildPlan() != null &&
                    event.builder.buildPlan().block == Blocks.thoriumReactor && event.builder.isPlayer() &&
                    event.team.cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                Player player = event.builder.getPlayer();
                String playerName = NetClient.colorizeName(player.id, player.name);
                if (interval.get(0, 300)) {
                    bundled("events.build-select.reactor-near-core", playerName, event.tile.x, event.tile.y);
                    Log.info(MessageFormat.format("{0} начал строить ториевый реактор близко к ядру ({1}, {2})", player.name, event.tile.x, event.tile.y));
                }
            }
        });
        // конец блока

        // блок "чат"
        Events.on(EventType.PlayerChatEvent.class, event -> {
            if (!event.message.startsWith("/")) {
                Groups.player.each(otherPlayer -> {
                    String msg = translateChat(event.player, otherPlayer, event.message);
                    otherPlayer.sendMessage(msg);
                });

                Log.info(MessageFormat.format(Const.CHAT_LOG_FORMAT, Strings.stripColors(event.player.name), Strings.stripColors(event.message), event.player.locale));
            }
        });
        // конец блока

        //блок "баны"
        Events.on(EventType.PlayerBanEvent.class, event -> {
            try {
                String ip = DBHandler.get(event.player.uuid(), Table.IP);
                netServer.admins.banPlayer(event.player.uuid());
                netServer.admins.banPlayerIP(event.player.ip());
                DBHandler.update(event.player.uuid(), Table.BANNED, true);
            } catch (SQLException e) {
                Log.err(e.getMessage());
            }
        });

        Events.on(EventType.PlayerIpBanEvent.class, event -> netServer.admins.banPlayerIP(event.ip));

        Events.on(EventType.PlayerUnbanEvent.class, event -> {
            try {
                String ip = DBHandler.get(event.player.uuid(), Table.IP);
                netServer.admins.unbanPlayerID(event.player.uuid());
                netServer.admins.unbanPlayerIP(event.player.ip());
                DBHandler.update(event.player.uuid(), Table.BANNED, false);
            } catch (SQLException e) {
                Log.err(e.getMessage());
            }
        });

        Events.on(EventType.PlayerIpUnbanEvent.class, event -> netServer.admins.unbanPlayerIP(event.ip));
        // конец блока

        // блок "история"
        Events.on(EventType.WorldLoadEvent.class, event -> {
            if (Groups.player.size() > 0 && autoPause) {
                state.serverPaused = false;
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

                for (int i = 0; i < entries.size && i < Const.LIST_PAGE_SIZE; i++) {
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

        handler.register("playtime", "<player/uuid>", "Check player playtime", (args) -> {
            Playerc player = Groups.player.find(p -> p.name.equals(args[0]));
            String uuid = player != null ? player.uuid() : args[0];

            try {
                Long time = DBHandler.get(uuid, Table.PLAY_TIME);
                if (time != null) {
                    StringBuilder result = new StringBuilder(player != null ? player.name() : args[0]);
                    result.append("plays").append(longToTime(time));
                    Log.info(result);
                } else {
                    Log.warn("Player/uuid not found!");
                }
            } catch (Throwable t) {
                Log.err(t);
            }
        });

        handler.register("auto-pause", "[on|off]", "Pause game with 0 people online", args -> {
            if (args.length == 0) {
                if (autoPause) {
                    Log.info("Авто-пауза включена");
                } else {
                    Log.info("Авто-пауза выключена");
                }
            } else if (args[0].equalsIgnoreCase("off")) {
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
            } else {
                Log.info("auto-pause: некорректное действие");
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.removeCommand("a");
        handler.<Player>register("a", "<text...>", "commands.admin.a.description", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
                Log.info(MessageFormat.format("{0} попытался отправить сообщение админам", Strings.stripColors(player.name)));
                return;
            }

            String message = args[0];
            Groups.player.each(Player::admin, otherPlayer -> {
                String msg = translateChat(player, otherPlayer, message);
                otherPlayer.sendMessage("<[scarlet]A[]>" + msg);
            });

            Log.info("<A>" + MessageFormat.format(Const.CHAT_LOG_FORMAT, Strings.stripColors(player.name), Strings.stripColors(message), player.locale));
        });

        handler.removeCommand("t");
        handler.<Player>register("t", "<text...>", "commands.t.description", (args, player) -> {
            String message = args[0];
            Groups.player.each(o -> o.team() == player.team(), otherPlayer -> {
                String msg = translateChat(player, otherPlayer, message);
                otherPlayer.sendMessage("<[#" + player.team().color + "]T[]>" + msg);
            });

            Log.info("<T>" + MessageFormat.format(Const.CHAT_LOG_FORMAT, Strings.stripColors(player.name), Strings.stripColors(message), player.locale));
        });

        handler.removeCommand("help");
        handler.<Player>register("help", "[page]", "commands.help.description", (args, player) -> {
            if (args.length > 0 && !Strings.canParseInt(args[0])) {
                bundled(player, "commands.page-not-int");
                return;
            }
            Locale locale = findLocale(player.locale);
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil(handler.getCommandList().size / Const.LIST_PAGE_SIZE);

            if (--page >= pages || page < 0) {
                bundled(player, "commands.under-page", pages);
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Bundle.format("commands.help.page", locale, page + 1, pages)).append("\n\n");

            for (int i = 6 * page; i < Math.min(6 * (page + 1), handler.getCommandList().size); i++) {
                CommandHandler.Command command = handler.getCommandList().get(i);
                if (command.description.startsWith("commands.admin") && !player.admin) continue; // скипаем админские команды
                result.append("[orange] /").append(command.text).append("[white] ")
                        .append(command.paramText)
                        .append("[lightgray] - ")
                        .append(Bundle.has(command.description, locale) ? Bundle.get(command.description, locale) : command.description)
                        .append("\n");
            }
            player.sendMessage(result.toString());
        });

        handler.<Player>register("tr", "[off|auto|double|somelocale]", "Настроить переводчик чата.", (args, player) -> {
            String locale;
            try {
                locale = DBHandler.get(player.uuid(), Table.TRANSLATOR);
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
                case "off" -> {
                    try {
                        DBHandler.update(player.uuid(), Table.TRANSLATOR, "off");
                    } catch (Throwable t) {
                        Log.err(t);
                    }
                    player.sendMessage("[sky]Перевод чата выключен.");
                }
                case "auto" -> {
                    try {
                        DBHandler.update(player.uuid(), Table.TRANSLATOR, "auto");
                    } catch (Throwable t) {
                        Log.err(t);
                    }
                    player.sendMessage("[sky]Перевод чата установлен в автоматический режим.");
                }
                case "double" -> {
                    try {
                        DBHandler.update(player.uuid(), Table.TRANSLATOR, "double");
                    } catch (Throwable t) {
                        Log.err(t);
                    }
                    player.sendMessage("[sky]Перевод чата установлен в автоматический режим c отображением оригинального сообщения.");
                }
                default -> {
                    Locale target = Structs.find(locales, l -> mode.equalsIgnoreCase(l.toString()));
                    if (target == null) {
                        player.sendMessage("[sky]Список доступных локализаций:\n" + Const.LocaleListHolder.LOCALE_LIST);
                        return;
                    }
                    try {
                        DBHandler.update(player.uuid(), Table.TRANSLATOR, target.toString());
                    } catch (Throwable t) {
                        Log.err(t);
                    }
                    player.sendMessage("[sky]Язык переводчика установлен на: " + target);
                }
            }
        });

        handler.<Player>register("rtv", "commands.rtv.description", (arg, player) -> {
            votesRTV.add(player.uuid());
            int cur = votesRTV.size();
            int req = (int) Math.ceil(Const.VOTES_RATIO * Groups.player.size());

            String playerName = NetClient.colorizeName(player.id, player.name);
            bundled("commands.rtv.vote", playerName, cur, req);

            if (cur >= req) {
                votesRTV.clear();
                bundled("commands.rtv.passed");
                Events.fire(new EventType.GameOverEvent(Team.crux));
            }
        });

        handler.<Player>register("version", "commands.version.description", (arg, player) -> bundled(player, "commands.version.msg",
                mods.list().find(l -> l.main instanceof ThedimasPlugin).meta.version));

        handler.<Player>register("discord", "Получить ссылку на Discord сервер", (args, player) -> player.sendMessage("https://discord.gg/RkbFYXFU9E"));

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

        handler.<Player>register("connect", "[list|server...]", "Подключиться к другому серверу", (args, player) -> {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
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

        handler.<Player>register("history", "[page] [detailed]", "Посмотреть историю блока", (args, player) -> {
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
                int pages = Mathf.ceil(entries.size / Const.LIST_PAGE_SIZE);

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

        handler.<Player>register("playtime", "commands.playtime.description", (args, player) -> {
            try {
                Long time = DBHandler.get(player.uuid(), Table.PLAY_TIME);
                if (time != null) {
                    bundled(player, "commands.playtime.msg", longToTime(time));
                }
            } catch (Throwable t) {
                Log.err(t);
            }
        });

        // блок "для админов"
        handler.<Player>register("admin", "Изменить свой статус", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
            } else {
                player.admin = !player.admin;
            }
        });

        handler.<Player>register("name", "[name...]","Изменить свое имя", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
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

        handler.<Player>register("spawn", "<unit> [count] [team]", "Заспавнить юнитов", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
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

        handler.<Player>register("team", "<team> [username...]", "Изменить команду", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
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

        handler.<Player>register("kill", "[username...]", "commands.admin.kill.description", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
                return;
            }

            if (args.length == 0) {
                player.unit().kill();
                bundled(player, "commands.admin.kill.suicide");

                Log.info(MessageFormat.format("{0} убил сам себя", Strings.stripColors(player.name)));
                return;
            }

            Player otherPlayer = Groups.player.find(p -> Strings.stripGlyphs(Strings.stripColors(p.name())).equalsIgnoreCase(args[0]));
            if (otherPlayer != null) {
                otherPlayer.unit().kill();
                String otherPlayerName = NetClient.colorizeName(otherPlayer.id, otherPlayer.name);
                bundled(player, "commands.admin.kill.kill-another", otherPlayerName);

                Log.info(MessageFormat.format("{0} убил {1}", Strings.stripColors(player.name), Strings.stripColors(otherPlayerName)));
            } else {
                player.sendMessage("[scarlet]Игрока с таким ником нет на сервере");
            }
        });

        handler.<Player>register("killall", "[team]", "Убить ВСЕХ", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
                return;
            }

            if (args.length == 0) {
                Groups.unit.each(Unitc::kill);
                player.sendMessage("[scarlet]Ты убил их всех... За что, Джонни?");

                Log.info(MessageFormat.format("{0} убил всех...", Strings.stripColors(player.name)));
            } else {
                Team team = Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[0]));
                if (team == null) {
                    player.sendMessage("[scarlet]Неверная команда. Возможные варианты:\n" + Const.TEAM_LIST);
                    return;
                }

                Groups.unit.each(u -> u.team == team, Unitc::kill); // Надеюсь, оно работает
                player.sendMessage("[scarlet]Ты убил всех с команды [#" + team.color + "]" + team + "... Их призраки буду преследовать тебя вечно!");

                Log.info(MessageFormat.format("{0} убил всех с команды {1}...", Strings.stripColors(player.name), team));
            }
        });

        handler.<Player>register("core", "<small|medium|big>", "commands.admin.core.description", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
                return;
            }

            Block core;
            switch (args[0].toLowerCase()) {
                case "small" -> core = Blocks.coreShard;
                case "medium" -> core = Blocks.coreFoundation;
                case "big" -> core = Blocks.coreNucleus;
                default -> {
                    bundled("commands.admin.core.core-type-not-found");
                    return;
                }
            }

            Tile tile = player.tileOn();
            Call.constructFinish(tile, core, player.unit(), (byte)0, player.team(), false);

            bundled(player, tile.block() == core, "commands.admin.core.success", "commands.admin.core.failed");

            Log.info(MessageFormat.format("{0} заспавнил ядро ({1}, {2})", Strings.stripColors(player.name), tile.x, tile.y));
        });

        handler.<Player>register("pause", "commands.admin.pause.description", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
                return;
            }
            Vars.state.serverPaused = !Vars.state.serverPaused;
            Log.info(MessageFormat.format("{0} поставил игру на паузу", Strings.stripColors(player.name)));
        });

        handler.<Player>register("end", "commands.admin.end.description", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
                return;
            }

            Events.fire(new EventType.GameOverEvent(Team.crux));
            Log.info(MessageFormat.format("{0} сменил карту принудительно", Strings.stripColors(player.name)));
        });
        // конец блока
    }

    public String longToTime(Long seconds) {
        long min = seconds / 60;
        long hour = min / 60;
        return String.format("%d:%02d:%02d", hour, min % 60, seconds % 60);
    }

    private String translateChat(Player player, Player otherPlayer, String message) {
        String locale = otherPlayer.locale;
        try {
            locale = DBHandler.get(otherPlayer.uuid(), Table.TRANSLATOR);
        } catch (Throwable t) {
            Log.err(t);
        }

        String translated = message;
        if (!otherPlayer.locale.equals(player.locale()) && !"off".equals(locale)) {
            try {
                String targetLocale = "auto".equals(locale) || "double".equals(locale) ? otherPlayer.locale : locale;
                translated = Translator.translate(message, targetLocale, "auto");
            } catch (Throwable t) {
                Log.err(t);
            }
        }

        String prefix = player.admin() ? "\uE82C" : "\uE872";
        String playerName = NetClient.colorizeName(player.id, player.name);

        return MessageFormat.format("double".equals(locale) ? Const.CHAT_FORMAT_DETAILED : Const.CHAT_FORMAT,
                prefix, playerName, translated, message);
    }

    public static void bundled(Player player, boolean condition, String keyTrue, String keyFalse, Object... values) {
        String key = condition ? keyTrue : keyFalse;
        player.sendMessage(Bundle.format(key, findLocale(player.locale), values));
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