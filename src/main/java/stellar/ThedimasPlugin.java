package stellar;

import arc.*;
import arc.math.*;
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
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.campaign.LaunchPad;
import mindustry.world.blocks.logic.LogicBlock;

import stellar.database.*;
import stellar.database.tables.Playtime;
import stellar.database.tables.Users;
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
        Log.info("ThedimasPlugin launched!");

        Core.settings.put(Const.SERVER_NAME_SETTING, Const.DEFAULT_SERVER_NAME);

        state.serverPaused = true;

        netServer.admins.addChatFilter((player, message) -> null);

        // ---------------------------------------ЗАГРУЗКА ЛОКАЛИЗАЦИЙ--------------------------------------- //
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
            Log.debug("Locales: @", Arrays.toString(Const.supportedLocales));
        }, Log::err);
        // -------------------------------------------------------------------------------------------------- //

        // -----------------------------------------ПУСКОВАЯ ПЛОЩАДКА---------------------------------------- //
        Events.on(EventType.PlayEvent.class, event -> state.rules.revealedBlocks.add(Blocks.launchPad));

        Timer.schedule(() -> Groups.build.each(b -> b.block instanceof LaunchPad, building -> {
            if (building.items.total() == 100 && building.power.status > 0.95) {
                Building core = building.closestCore();
                if (core == null) {
                    return;
                }

                for (int i = 0; i < building.items.length(); i++) {
                    Item item = content.items().get(i);
                    if (building.items.has(item) && !core.acceptItem(building, item)) {
                        return;
                    }
                }

                float thisX = building.x;
                float thisY = building.y;
                float coreX = core.x;
                float coreY = core.y;

                float angle = Angles.angle(thisX, thisY, coreX, coreY);

                float a = Math.abs(thisX - coreX);
                float b = Math.abs(thisY - coreY);
                float c = (float) Math.hypot(a, b);

                BulletType bullet = Bullets.artilleryDense;
                float baseSpeed = bullet.speed;
                float baseLifetime = bullet.lifetime;

                float lifetime = c / baseSpeed / baseLifetime;

                Call.clearItems(building);
                Call.createBullet(bullet, building.team, thisX, thisY, angle, 0F, 1F, lifetime);
                Timer.schedule(() -> building.items.each((item, amount) -> core.items.add(item, amount)),
                        lifetime * 60);
            }
        }), 0, 0.1F);
        // -------------------------------------------------------------------------------------------------- //

        // ---------------------------------------ОБНОВЛЕНИЕ ПЛЕЙТАЙМА--------------------------------------- //
        Events.run(EventType.Trigger.update, () -> {
            if (interval.get(1, 3600)) { // 1 минута
                if (Playtime.FIELDS.containsKey(Core.settings.getString(Const.SERVER_NAME_SETTING))) {
                    for (Player p : Groups.player) {
                        try {
                            String serverName = Core.settings.getString(Const.SERVER_NAME_SETTING);
                            Long time = DBHandler.get(p.uuid(), Playtime.FIELDS.get(serverName.toLowerCase()));
                            Objects.requireNonNull(time, "time");
                            DBHandler.update(p.uuid(), Playtime.FIELDS.get(Const.SERVER_NAME_SETTING), time + 60);
                        } catch (Throwable t) {
                            Log.err(t);
                        }
                    }
                } else {
                    Log.err("Имя сервера не существует в базе данных!");
                }
            }
        });
        // -------------------------------------------------------------------------------------------------- //

        // -------------------------------------------ПРИСОЕДИНЕНИЕ------------------------------------------ //
        Events.on(EventType.PlayerJoin.class, event -> {
            if (Groups.player.size() >= 1 && autoPause && state.serverPaused) {
                state.serverPaused = false;
                Log.info("auto-pause: @ player(s) connected -> Game unpaused...", Groups.player.size());
            }

            Log.info(Const.JOIN_LOG_FORMAT, event.player.name, event.player.locale, event.player.con.address);
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
                if (DBHandler.userExist(event.player.uuid())) {
                    DBHandler.update(event.player.uuid(), Users.NAME, event.player.name);
                    DBHandler.update(event.player.uuid(), Users.LOCALE, event.player.locale);
                    DBHandler.update(event.player.uuid(), Users.IP, event.player.ip());

                    Boolean banned = DBHandler.get(event.player.uuid(), Users.BANNED);
                    if(banned != null && banned) {
                        netServer.admins.banPlayer(event.player.uuid());
                        netServer.admins.banPlayerIP(event.player.ip());
                    } else {
                        Boolean admin = DBHandler.get(event.player.uuid(), Users.ADMIN);
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
        // -------------------------------------------------------------------------------------------------- //

        // ---------------------------------------------ОТКЛЮЧЕНИЕ------------------------------------------- //
        Events.on(EventType.PlayerLeave.class, event -> {
            if (Groups.player.size() - 1 < 1 && autoPause) {
                state.serverPaused = true;
                Log.info("auto-pause: @ player connected -> Game paused...", Groups.player.size() - 1);
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
        // -------------------------------------------------------------------------------------------------- //

        Events.on(EventType.ServerLoadEvent.class, event -> Log.info("ThedimasPlugin: Server loaded"));

        Events.on(EventType.GameOverEvent.class, e -> votesRTV.clear());

        // ----------------------------------------------ТОРИЙКИ--------------------------------------------- //
        Events.on(EventType.DepositEvent.class, event -> {
            Player target = event.player;
            Building building = event.tile;

            if(building.block() == Blocks.thoriumReactor && event.item == Items.thorium &&
                    target.team().cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                String playerName = NetClient.colorizeName(event.player.id, event.player.name);
                bundled("events.deposit.thorium-in-reactor", playerName, building.tileX(), building.tileY());
                Log.info("@ положил торий в реактор (@, @)", target.name, building.tileX(), building.tileY());
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
                    Log.info("@ начал строить ториевый реактор близко к ядру (@, @)", player.name, event.tile.x, event.tile.y);
                }
            }
        });
        // -------------------------------------------------------------------------------------------------- //

        // ------------------------------------------------ЧАТ----------------------------------------------- //
        Events.on(EventType.PlayerChatEvent.class, event -> {
            if (!event.message.startsWith("/")) {
                Groups.player.each(otherPlayer -> {
                    String msg = translateChat(event.player, otherPlayer, event.message);
                    otherPlayer.sendMessage(msg);
                });

                Log.info(Const.CHAT_LOG_FORMAT, Strings.stripColors(event.player.name), Strings.stripColors(event.message), event.player.locale);
            }
        });
        // -------------------------------------------------------------------------------------------------- //

        // -----------------------------------------------БАНЫ----------------------------------------------- //
        Events.on(EventType.PlayerBanEvent.class, event -> {
            try {
                String ip = DBHandler.get(event.player.uuid(), Users.IP);
                netServer.admins.banPlayer(event.player.uuid());
                netServer.admins.banPlayerIP(event.player.ip());
                DBHandler.update(event.player.uuid(), Users.BANNED, true);
            } catch (SQLException e) {
                Log.err(e.getMessage());
            }
        });

        Events.on(EventType.PlayerIpBanEvent.class, event -> netServer.admins.banPlayerIP(event.ip));

        Events.on(EventType.PlayerUnbanEvent.class, event -> {
            try {
                String ip = DBHandler.get(event.player.uuid(), Users.IP);
                netServer.admins.unbanPlayerID(event.player.uuid());
                netServer.admins.unbanPlayerIP(event.player.ip());
                DBHandler.update(event.player.uuid(), Users.BANNED, false);
            } catch (SQLException e) {
                Log.err(e.getMessage());
            }
        });

        Events.on(EventType.PlayerIpUnbanEvent.class, event -> netServer.admins.unbanPlayerIP(event.ip));
        // -------------------------------------------------------------------------------------------------- //

        // ----------------------------------------------ИСТОРИЯ--------------------------------------------- //
        Events.on(EventType.WorldLoadEvent.class, event -> {
            if (Groups.player.size() > 0 && autoPause) {
                state.serverPaused = false;
                Log.info("auto-pause: @ player(s) connected -> Game unpaused...", Groups.player.size());
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
                Locale locale = findLocale(event.player.locale);
                result.append(Bundle.format("history.page", locale, x, y)).append("\n");

                entries.cleanUp();
                if (entries.isEmpty()) {
                    result.append(Bundle.get("history.empty", locale)).append("\n");
                }

                for (int i = 0; i < entries.size && i < Const.LIST_PAGE_SIZE; i++) {
                    HistoryEntry entry = entries.get(i);

                    result.append(entry.getMessage(locale));
                    if (detailed) {
                        result.append(Bundle.format("history.last-access-time", locale, entry.getLastAccessTime(TimeUnit.MINUTES)));
                    }
                    result.append("\n");
                }

                event.player.sendMessage(result.toString());
            }
        });
        // -------------------------------------------------------------------------------------------------- //
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
                    Log.err("Unable to export data of player @ (@)", Strings.stripColors(info.lastName), Strings.stripColors(info.id));
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
            } else if (args[0].equalsIgnoreCase("off")) {
                autoPause = false;
                Log.info("Авто-пауза выключена");

                Vars.state.serverPaused = false;
                Log.info("auto-pause: @ игрок(ов) онлайн -> Игра снята с паузы...", Groups.player.size());
            } else if (args[0].equalsIgnoreCase("on")) {
                autoPause = true;
                Log.info("Авто-пауза включена");

                if (Groups.player.size() < 1 && autoPause) {
                    Vars.state.serverPaused = true;
                    Log.info("auto-pause: @ игроков онлайн -> Игра поставлена на паузу...", Groups.player.size());
                }
            } else {
                Log.info("auto-pause: некорректное действие");
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        // ------------------------------------------------ЧАТ----------------------------------------------- //
        handler.removeCommand("a");
        handler.<Player>register("a", "<text...>", "commands.admin.a.description", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
                Log.info("@ попытался отправить сообщение админам", Strings.stripColors(player.name));
                return;
            }

            String message = args[0];
            Groups.player.each(Player::admin, otherPlayer -> {
                String msg = translateChat(player, otherPlayer, message);
                otherPlayer.sendMessage("<[scarlet]A[]>" + msg);
            });

            Log.info("<A>" + Const.CHAT_LOG_FORMAT, Strings.stripColors(player.name), Strings.stripColors(message), player.locale);
        });

        handler.removeCommand("t");
        handler.<Player>register("t", "<text...>", "commands.t.description", (args, player) -> {
            String message = args[0];
            Groups.player.each(o -> o.team() == player.team(), otherPlayer -> {
                String msg = translateChat(player, otherPlayer, message);
                otherPlayer.sendMessage("<[#" + player.team().color + "]T[]>" + msg);
            });

            Log.info("<T>" + Const.CHAT_LOG_FORMAT, Strings.stripColors(player.name), Strings.stripColors(message), player.locale);
        });
        // -------------------------------------------------------------------------------------------------- //

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

        handler.<Player>register("tr", "[off|auto|double|somelocale]", "commands.tr.description", (args, player) -> {
            String locale;
            try {
                locale = DBHandler.get(player.uuid(), Users.TRANSLATOR);
            } catch (Throwable t) {
                bundled(player, "commands.tr.error");
                Log.err(t);
                return;
            }

            if (args.length == 0) {
                bundled(player, "commands.tr.current", locale);
                return;
            }

            String mode = args[0].toLowerCase();
            switch (mode) {
                case "off" -> {
                    try {
                        DBHandler.update(player.uuid(), Users.TRANSLATOR, "off");
                    } catch (Throwable t) {
                        Log.err(t);
                    }
                    bundled(player, "commands.tr.disabled");
                }
                case "auto" -> {
                    try {
                        DBHandler.update(player.uuid(), Users.TRANSLATOR, "auto");
                    } catch (Throwable t) {
                        Log.err(t);
                    }
                    bundled(player, "commands.tr.auto");
                }
                case "double" -> {
                    try {
                        DBHandler.update(player.uuid(), Users.TRANSLATOR, "double");
                    } catch (Throwable t) {
                        Log.err(t);
                    }
                    bundled(player, "commands.tr.double");
                }
                default -> {
                    Locale target = Structs.find(locales, l -> mode.equalsIgnoreCase(l.toString()));
                    if (target == null) {
                        bundled(player, "commands.tr.list", Const.LocaleListHolder.LOCALE_LIST);
                        return;
                    }
                    try {
                        DBHandler.update(player.uuid(), Users.TRANSLATOR, target.toString());
                    } catch (Throwable t) {
                        Log.err(t);
                    }
                    bundled(player, "commands.tr.set", target);
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

        handler.<Player>register("discord", "commands.discord.description", (args, player) -> player.sendMessage("https://discord.gg/RkbFYXFU9E"));

        handler.<Player>register("rules", "commands.rules.description", (args, player) -> {
            if (player.locale.startsWith("uk")) {
                player.sendMessage(Const.RULES_UK);
            } else if (player.locale.startsWith("ru")) {
                player.sendMessage(Const.RULES_RU);
            } else {
                player.sendMessage(Const.RULES_EN);
            }
        });

        handler.<Player>register("hub", "commands.hub.description", (args, player) -> {
            String[] address = Const.SERVER_ADDRESS.get("hub").split(":");
            String ip = address[0];
            int port = Integer.parseInt(address[1]);

            Call.connect(player.con, ip, port);
        });

        handler.<Player>register("connect", "[list|server...]", "commands.connect.description", (args, player) -> {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                bundled(player, "commands.connect.list", Const.SERVER_LIST);
                return;
            }

            String serverName = args[0].toLowerCase();
            if (!Const.SERVER_ADDRESS.containsKey(serverName)) {
                bundled(player, "commands.connect.server-notfound", Const.SERVER_LIST);
                return;
            }

            String[] address = Const.SERVER_ADDRESS.get(serverName).split(":");
            String ip = address[0];
            int port = Integer.parseInt(address[1]);
            Vars.net.pingHost(ip, port, host -> Call.connect(player.con, ip, port), e -> bundled(player, "commands.connect.serve-offline"));
        });

        handler.<Player>register("history", "[page] [detailed]", "commands.history.description", (args, player) -> {
            boolean detailed = args.length == 2 && Structs.contains(Const.BOOL_VALUES.split(", "), args[1].toLowerCase());

            if (args.length > 0 && activeHistoryPlayers.containsKey(player.uuid())) {
                if (!Strings.canParseInt(args[0])) {
                    bundled(player, "commands.page-not-int");
                    return;
                }

                int mouseX = Mathf.clamp(Mathf.round(player.mouseX / 8), 1, world.width());
                int mouseY = Mathf.clamp(Mathf.round(player.mouseY / 8), 1, world.height());
                Locale locale = findLocale(player.locale);

                CacheSeq<HistoryEntry> entries = getHistorySeq(mouseX, mouseY);

                int page = Integer.parseInt(args[0]) - 1;
                int pages = Mathf.ceil(entries.size / Const.LIST_PAGE_SIZE);

                if (page >= pages || pages < 0 || page < 0) {
                    bundled(player, "commands.under-page", page);
                    return;
                }

                StringBuilder result = new StringBuilder();
                result.append(Bundle.format("commands.history.page", locale, mouseX, mouseY, page + 1, pages)).append("\n");

                if (entries.isEmpty()) {
                    result.append(Bundle.get("history.empty", locale)).append("\n");
                }

                for (int i = 6 * page; i < Math.min(6 * (page + 1), entries.size); i++) {
                    HistoryEntry entry = entries.get(i);
                    result.append(entry.getMessage(locale));
                    if (detailed) {
                        result.append(Bundle.format("history.last-access-time", locale, entry.getLastAccessTime(TimeUnit.MINUTES)));
                    }
                    result.append("\n");
                }

                player.sendMessage(result.toString());
            } else if (activeHistoryPlayers.containsKey(player.uuid())) {
                activeHistoryPlayers.remove(player.uuid());
                bundled(player, "commands.history.detailed.disabled");
            } else if (args.length == 2) {
                activeHistoryPlayers.put(player.uuid(), detailed);
                String msg = detailed ? "commands.history.detailed" : "commands.history.default";
                bundled(player, "commands.history.enabled", msg);
            } else {
                activeHistoryPlayers.put(player.uuid(), false);
                bundled(player, "commands.history.disabled");
            }
        });

        handler.<Player>register("playtime", "[server...]", "commands.playtime.description", (args, player) -> {
            String serverName;
            if (args.length > 0) {
                serverName = args[0].toLowerCase();
            } else {
                serverName = Core.settings.getString(Const.SERVER_NAME_SETTING).toLowerCase();
                if (serverName.equals(Const.DEFAULT_SERVER_NAME)) {
                    player.sendMessage("[scarlet]Ошибка! Обратитесь, пожалуйста, к администрации.");
                    return;
                }
            }

            if (!Playtime.FIELDS.containsKey(serverName)) {
                // TODO: исправить key в бандле
                bundled(player, "commands.connect.server-notfound", Const.SERVER_LIST);
                return;
            }

            try {
                Long time = DBHandler.get(player.uuid(), Playtime.FIELDS.get(serverName));
                if (time != null) {
                    // TODO: изменить бандл (учитывать выбранный сервер)
                    bundled(player, "commands.playtime.msg", longToTime(time));
                }
            } catch (Throwable t) {
                Log.err(t);
            }
        });

        // блок "для админов"
        handler.<Player>register("admin", "commands.admin.admin.description", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
            } else {
                player.admin = !player.admin;
            }
        });

        handler.<Player>register("name", "[name...]","commands.admin.name.description", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
                return;
            }

            if (args.length == 0) {
                player.name(admins.get(player.uuid()));
                String playerName = NetClient.colorizeName(player.id, player.name);
                bundled(player, "commands.admin.name.reset", playerName);
            } else {
                player.name(args[0]);
                String playerName = NetClient.colorizeName(player.id, player.name);
                bundled(player, "commands.admin.name.update", playerName);
            }
        });

        handler.<Player>register("spawn", "<unit> [count] [team]", "commands.admin.unit.description", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
                return;
            }

            UnitType unit = Vars.content.units().find(b -> b.name.equalsIgnoreCase(args[0]));
            if (unit == null) {
                bundled(player, "commands.admin.unit.notfound", Const.UNIT_LIST);
                return;
            }

            int count = args.length > 1 && Strings.canParseInt(args[1]) ? Strings.parseInt(args[1]) : 1;
            if (count > 24) {
                bundled(player, "commands.admin.unit.under-limit");
                return;
            } else if (count < 1) {
                bundled(player, "commands.admin.unit.negative-count");
                return;
            }

            Team team = args.length > 2 ? Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[2])) : player.team();
            if (team == null) {
                bundled(player, "commands.admin.unit.team-notfound", Const.TEAM_LIST);
                return;
            }

            for (int i = 0; count > i; i++) {
                unit.spawn(team, player.x, player.y);
            }

            bundled(player, "commands.admin.unit.text", count, unit, team.color, team);
        });

        handler.<Player>register("team", "<team> [username...]", "commands.admin.team.description", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
                return;
            }

            Team team = Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[0]));
            if (team == null) {
                bundled(player, "commands.admin.team.notfound", Const.TEAM_LIST);
                return;
            }

            if (args.length == 1) {
                player.team(team);
                bundled(player, "commands.admin.team.changed", team.color, team);
            } else {
                Player otherPlayer = Groups.player.find(p -> Strings.stripGlyphs(Strings.stripColors(p.name())).equalsIgnoreCase(args[1]));
                if (otherPlayer != null) {
                    otherPlayer.team(team);
                    bundled(otherPlayer, "commands.admin.team.updated", team.color, team);

                    String otherPlayerName = NetClient.colorizeName(otherPlayer.id, otherPlayer.name);
                    bundled(player, "commands.admin.team.successful-updated", otherPlayer, team.color, team);
                } else {
                    bundled(player, "commands.admin.team.player-notfound");
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

                Log.info("@ убил сам себя", Strings.stripColors(player.name));
                return;
            }

            Player otherPlayer = Groups.player.find(p -> Strings.stripGlyphs(Strings.stripColors(p.name())).equalsIgnoreCase(args[0]));
            if (otherPlayer != null) {
                otherPlayer.unit().kill();
                String otherPlayerName = NetClient.colorizeName(otherPlayer.id, otherPlayer.name);
                bundled(player, "commands.admin.kill.kill-another", otherPlayerName);

                Log.info("@ убил @", Strings.stripColors(player.name), Strings.stripColors(otherPlayerName));
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
                bundled(player, "commands.admin.killall.text");

                Log.info("@ убил всех...", Strings.stripColors(player.name));
            } else {
                Team team = Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[0]));
                if (team == null) {
                    bundled(player, "commands.admin.killall.team-notfound", Const.TEAM_LIST);
                    return;
                }

                Groups.unit.each(u -> u.team == team, Unitc::kill); // Надеюсь, оно работает
                bundled(player, "commands.admin.killall.text-teamed", team.color, team);

                Log.info("@ убил всех с команды @...", Strings.stripColors(player.name), team);
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

            Log.info("@ заспавнил ядро (@, @)", Strings.stripColors(player.name), tile.x, tile.y);
        });

        handler.<Player>register("pause", "commands.admin.pause.description", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
                return;
            }
            Vars.state.serverPaused = !Vars.state.serverPaused;
            Log.info("@ поставил игру на паузу", Strings.stripColors(player.name));
        });

        handler.<Player>register("end", "commands.admin.end.description", (args, player) -> {
            if (!admins.containsKey(player.uuid())) {
                bundled(player, "commands.access-denied");
                return;
            }

            Events.fire(new EventType.GameOverEvent(Team.crux));
            Log.info("@ сменил карту принудительно", Strings.stripColors(player.name));
        });
        // конец блока
    }

    public String longToTime(long seconds) {
        long min = seconds / 60;
        long hour = min / 60;
        return String.format("%d:%02d:%02d", hour, min % 60, seconds % 60);
    }

    private String translateChat(Player player, Player otherPlayer, String message) {
        String locale = otherPlayer.locale;
        try {
            locale = DBHandler.get(otherPlayer.uuid(), Users.TRANSLATOR);
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
