package stellar;

import arc.*;
import arc.graphics.Color;
import arc.math.*;
import arc.struct.Seq;
import arc.util.*;
import arc.util.Timer;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.content.Items;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.Administration;
import mindustry.net.Packets;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.campaign.LaunchPad;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.power.PowerNode;

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
import stellar.util.logger.DiscordLogger;
import stellar.util.Translator;
import stellar.command.*;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.*;

@SuppressWarnings({"unused", "unchecked"})
public class ThedimasPlugin extends Plugin {

    public boolean autoPause = true;
    public boolean rtv = true;
    private int waves = 0;

    private final Interval interval = new Interval(2);

    public final Set<String> votesRTV = new HashSet<>();

    public CacheSeq<HistoryEntry>[][] history;

    // uuid -> enable
    public final Map<String, Boolean> activeHistoryPlayers = new HashMap<>();

    // uuid -> name
    public final Map<String, String> admins = new HashMap<>();

    public CacheSeq<HistoryEntry> getHistorySeq(int x, int y) {
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

        state.serverPaused = true;
        netServer.admins.addChatFilter((player, message) -> null);

        // region загрузка локализаций
        Http.get(ghApi + "/search/code?q=name+repo:Anuken/Mindustry+filename:bundle&per_page=100", res -> {
            Jval json = Jval.read(res.getResultAsString());
            Seq<String> names = json.get("items").asArray().map(obj -> obj.getString("name"))
                    .filter(str -> str.endsWith(".properties") && !str.equals("bundle.properties"))
                    .map(str -> str.substring("bundle".length() + 1, str.lastIndexOf('.')))
                    .add("en");

            locales = new Locale[names.size];
            for (int i = 0; i < locales.length; i++) {
                locales[i] = parseLocale(names.get(i));
            }

            Arrays.sort(locales, Structs.comparing(l -> l.getDisplayName(l), String.CASE_INSENSITIVE_ORDER));
            locales = Seq.with(locales).add(new Locale("router")).toArray(Locale.class);
            Log.debug("Fetched locales: @", Arrays.toString(Const.supportedLocales));
        }, Log::err);
        // endregion

        // region пусковая площадка
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

                Call.effect(Fx.launchPod, building.tile.drawx(), building.tile.drawy(), 0, Color.white);
                building.items.each((item, amount) -> core.items.add(item, amount));
                Call.clearItems(building);
            }
        }), 0, 0.1F);
        // endregion

        //region обновление плейтайма
        Events.run(EventType.Trigger.update, () -> {
            if (interval.get(1, 3600)) { // 1 минута
                if (Playtime.FIELDS.containsKey(Const.SERVER_COLUMN_NAME)) {
                    for (Player p : Groups.player) {
                        try {
                            Long time = DBHandler.get(p.uuid(), Playtime.FIELDS.get(Const.SERVER_COLUMN_NAME));
                            if (time == null) {
                                Log.err("Player '" + p.uuid() + "' doesn't exists");
                                DiscordLogger.err("Player '" + p.uuid() + "' doesn't exists");
                            }
                            long computed = (time != null ? time : 0) + 60;
                            DBHandler.update(p.uuid(), Playtime.FIELDS.get(Const.SERVER_COLUMN_NAME), computed);
                        } catch (Throwable t) {
                            Log.err("Failed to update playtime for player '" + p.uuid() + "'", t);
                            DiscordLogger.err("Failed to update playtime for player '" + p.uuid() + "'", t);
                        }
                    }
                } else {
                    Log.err("Сервер @ не существует в базе данных!", Const.SERVER_COLUMN_NAME);
                    DiscordLogger.err("Сервер " + Const.SERVER_COLUMN_NAME + " не существует в базе данных!");
                }
            }
        });
        // endregion

        // region PlayerConnect
        Events.on(EventType.PlayerConnect.class, event -> {
            String uuid = event.player.uuid();
            String name = event.player.name;
            try {
                if (DBHandler.userExist(uuid)) {
                    Boolean banned = DBHandler.get(uuid, Users.BANNED);
                    if (banned != null) {
                        if (banned) {
                            event.player.kick(Packets.KickReason.banned);
                        }
                    }
                }
            } catch (SQLException e) {
                Log.err(e);
                DiscordLogger.err(e);
            }

            for (String pirate : Const.PIRATES) {
                if (name.toLowerCase().contains(pirate)) {
                    event.player.con.kick(Bundle.get("events.join.player-pirate", ThedimasPlugin.findLocale(event.player.locale)));
                    break;
                }
            }
        });
        // endregion

        // region PlayerJoin
        Events.on(EventType.PlayerJoin.class, event -> {
            if (Groups.player.size() >= 1 && autoPause && state.serverPaused) {
                state.serverPaused = false;
                Log.info("auto-pause: @ player(s) connected -> Game unpaused...", Groups.player.size());
            }

            Log.info(Const.JOIN_LOG_FORMAT, event.player.name, event.player.locale, event.player.con.address);
            String playerName = event.player.coloredName();
            bundled("events.join.player-join", playerName);

            Locale locale = findLocale(event.player.locale);
            String rules = Bundle.get("rules", locale);
            String welcome = Bundle.format("welcome", locale, rules, Const.DISCORD_INVITE);
            Call.infoMessage(event.player.con, welcome);

            try {
                if (DBHandler.userExist(event.player.uuid())) {
                    DBHandler.update(event.player.uuid(), Users.NAME, event.player.name);
                    DBHandler.update(event.player.uuid(), Users.LOCALE, event.player.locale);
                    DBHandler.update(event.player.uuid(), Users.IP, event.player.ip());

                    Boolean admin = DBHandler.get(event.player.uuid(), Users.ADMIN);
                    if (admin != null && admin) {
                        admins.put(event.player.uuid(), event.player.name);
                        event.player.admin = true;
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
                DiscordLogger.err(e);
            }
        });
        // endregion

        // region баны
        Events.on(EventType.PlayerBanEvent.class, event -> {
            try {
                DBHandler.update(event.uuid, Users.BANNED, true);
            } catch (SQLException e) {
                Log.err("Failed to ban uuid for player '" + event.uuid + "'", e);
                DiscordLogger.err("Failed to ban uuid for player '" + event.uuid + "'", e);
            }
        });

        Events.on(EventType.PlayerIpBanEvent.class, event -> {
            Player target = Groups.player.find(p -> p.ip().equalsIgnoreCase(event.ip));
            if (target == null) {
                Log.err("No player with ip '@' found.", event.ip);
                return;
            }

            String uuid = target.uuid();
            try {
                DBHandler.update(uuid, Users.BANNED, true);
            } catch (SQLException e) {
                Log.err("Failed to ban ip for player '" + uuid + "'", e);
                DiscordLogger.err("Failed to ban ip for player '" + uuid + "'", e);
            }
        });

        Events.on(EventType.PlayerUnbanEvent.class, event -> {
            try {
                DBHandler.update(event.uuid, Users.BANNED, false);
            } catch (SQLException e) {
                Log.err("Failed to unban uuid for player '" + event.uuid + "'", e);
                DiscordLogger.err("Failed to unban uuid for player '" + event.uuid + "'", e);
            }
        });

        Events.on(EventType.PlayerIpUnbanEvent.class, event -> {
            Player target = Groups.player.find(p -> p.ip().equalsIgnoreCase(event.ip));
            if (target == null) {
                Log.err("No player with ip '@' found.", event.ip);
                return;
            }

            String uuid = target.uuid();
            try {
                DBHandler.update(uuid, Users.BANNED, false);
            } catch (SQLException e) {
                Log.err("Failed to unban ip for player '" + uuid + "'", e);
                DiscordLogger.err("Failed to unban ip for player '" + uuid + "'", e);
            }
        });
        // endregion

        // region отключение
        Events.on(EventType.PlayerLeave.class, event -> {
            if (Groups.player.size() - 1 < 1 && autoPause) {
                state.serverPaused = true;
                Log.info("auto-pause: @ player connected -> Game paused...", Groups.player.size() - 1);
            }

            if (votesRTV.contains(event.player.uuid())) {
                votesRTV.remove(event.player.uuid());
                int cur = votesRTV.size();
                int req = (int) Math.ceil(Const.VOTES_RATIO * Groups.player.size());
                String playerName = event.player.coloredName();
                bundled("commands.rtv.leave", playerName, cur, req);
            }

            admins.remove(event.player.uuid());
            activeHistoryPlayers.remove(event.player.uuid());

            Log.info(event.player.name + " has disconnected from the server");
            String playerName = event.player.coloredName();
            bundled("events.leave.player-leave", playerName);
        });
        // endregion

        Events.on(EventType.ServerLoadEvent.class, event -> Log.info("ThedimasPlugin: Server loaded"));

        Events.on(EventType.GameOverEvent.class, event -> votesRTV.clear());

        // region ториевые реакторы
        Events.on(EventType.DepositEvent.class, event -> {
            Player target = event.player;
            Building building = event.tile;

            if (building.block() == Blocks.thoriumReactor && event.item == Items.thorium
                    && target.team().cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                String playerName = event.player.coloredName();
                bundled("events.deposit.thorium-in-reactor", playerName, building.tileX(), building.tileY());

                Log.info("@ положил торий в реактор (@, @)", target.name, building.tileX(), building.tileY());
                DiscordLogger.warn(String.format("%s положил торий в реактор (%f, %f)", player.name, event.tile.x, event.tile.y));
            }
        });

        Events.on(EventType.BuildSelectEvent.class, event -> {
            if (!event.breaking && event.builder != null && event.builder.buildPlan() != null
                    && event.builder.buildPlan().block == Blocks.thoriumReactor && event.builder.isPlayer()
                    && event.team.cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                Player player = event.builder.getPlayer();
                String playerName = player.coloredName();
                if (interval.get(0, 300)) {
                    bundled("events.build-select.reactor-near-core", playerName, event.tile.x, event.tile.y);

                    Log.info("@ начал строить ториевый реактор близко к ядру (@, @)", player.name, event.tile.x, event.tile.y);
                    DiscordLogger.warn(String.format("%s начал строить ториевый реактор близко к ядру (%d, %d)", player.name, event.tile.x, event.tile.y));
                }
            }
        });
        // endregion

        // region чат
        Events.on(EventType.PlayerChatEvent.class, event -> {
            if (!event.message.startsWith("/")) {
                Groups.player.each(otherPlayer -> {
                    String msg = translateChat(event.player, otherPlayer, event.message);
                    otherPlayer.sendMessage(msg);
                });

                Log.info(Const.CHAT_LOG_FORMAT, Strings.stripColors(event.player.name), Strings.stripColors(event.message), event.player.locale);
            }
        });
        // endregion

        Events.on(EventType.WorldLoadEvent.class, event -> {
            if (Groups.player.size() > 0 && autoPause) { // автопауза
                state.serverPaused = false;
                Log.info("auto-pause: @ player(s) connected -> Game unpaused...", Groups.player.size());
            }

            history = new CacheSeq[world.width()][world.height()];
        });

        // region история
        netServer.admins.addActionFilter(action -> {
            if (action.type == Administration.ActionType.rotate) {
                HistoryEntry entry = new RotateEntry(action.player, action.tile.build.block, action.rotation);
                getHistorySeq(action.tile.x, action.tile.y).add(entry);
            }
            return true;
        });

        Events.on(EventType.BlockBuildEndEvent.class, event -> {
            if (event.tile.build == null) return; // игнорируем ломание/строительство блоков по типу валунов

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

                Seq<Building> conns = event.tile.getPowerConnections(new Seq<>());
                connect = lastConfigEntry.value instanceof Long &&
                        (conns.any() && event.tile.block instanceof PowerNode &&
                                conns.size > Pack.leftInt((Long) lastConfigEntry.value) ||
                                event.value instanceof Integer && (int) event.value >= 0);
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
                        result.append(Bundle.format("history.timestamp", locale, entry.getTimestamp(TimeUnit.MINUTES)));
                    }
                    result.append("\n");
                }

                event.player.sendMessage(result.toString());
            }
        });
        // endregion

        // region опыт
        Events.on(EventType.GameOverEvent.class, event -> {
            Gamemode gamemode = state.rules.mode();
            Team winner = event.winner;
            switch (gamemode) {
                case pvp -> {
                    for (Player p : Groups.player) {
                        int currExp;
                        try {
                            currExp = DBHandler.get(p.uuid(), Users.EXP);
                        } catch (SQLException e) {
                            Log.err(e);
                            continue;
                        }
                        if (p.team() == winner) {
                            try {
                                DBHandler.update(p.uuid(), Users.EXP, currExp + 1000);
                            } catch (SQLException | NullPointerException e) {
                                Log.err(e);
                            }
                        } else if (p.team() != Team.derelict) {
                            try {
                                DBHandler.update(p.uuid(), Users.EXP, (currExp < 200) ? currExp - 200 : 0);
                            } catch (SQLException | NullPointerException e) {
                                Log.err(e);
                            }
                        }
                    }
                }
                case attack -> {
                    for (Player p : Groups.player) {
                        int currExp;
                        try {
                            currExp = DBHandler.get(p.uuid(), Users.EXP);
                        } catch (SQLException e) {
                            Log.err(e);
                            continue;
                        }
                        try {
                            if (winner == Team.sharded) {
                                DBHandler.update(p.uuid(), Users.EXP, currExp + 500);
                            } else if (winner != Team.derelict) {
                                DBHandler.update(p.uuid(), Users.EXP, (currExp < 100) ? currExp - 100 : 0);
                            }
                        } catch (SQLException e) {
                            Log.err(e);
                        }
                    }
                }
                case survival -> {
                    if (waves > 25) {
                        for (Player p : Groups.player) {
                            int currExp;
                            try {
                                currExp = DBHandler.get(p.uuid(), Users.EXP);
                                DBHandler.update(p.uuid(), Users.EXP, currExp + waves * 10);
                            } catch (SQLException e) {
                                Log.err(e);
                            }
                        }
                    }
                }
            }
            waves = 0;
        });

        Events.on(EventType.WaveEvent.class, event -> {
            waves++;
            Gamemode gamemode = state.rules.mode();
            switch (gamemode) {
                case survival -> {
                    for (Player p : Groups.player) {
                        int currExp;
                        try {
                            currExp = DBHandler.get(p.uuid(), Users.EXP);
                            DBHandler.update(p.uuid(), Users.EXP, currExp + 10 * waves%10==0 ? 10 : 1);
                        } catch (SQLException e) {
                            Log.err(e);
                        }
                    }
                }
                case attack -> {
                    if (!(waves>10)) {
                        break;
                    }
                    for (Player p : Groups.player) {
                        int currExp;
                        try {
                            currExp = DBHandler.get(p.uuid(), Users.EXP);
                            DBHandler.update(p.uuid(), Users.EXP, (currExp < 10) ? currExp - 10 : 0);
                        } catch (SQLException e) {
                            Log.err(e);
                        }
                    }
                }
            }
        });
        // endregion
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        ServerCommands.load(handler);
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        PlayerCommands.load(handler);
        AdminCommands.load(handler);
    }

    public static Player findPlayer(String name) {
        String replacedName = name.replace('_', ' ');
        return Groups.player.find(p -> name.equals(Strings.stripColors(p.name))
                || name.equals(stripColorsAndGlyphs(p.name))
                || replacedName.equals(Strings.stripColors(p.name))
                || replacedName.equals(stripColorsAndGlyphs(p.name)));
    }

    public static String stripColorsAndGlyphs(String str) {
        str = Strings.stripColors(str);

        // because Strings.stripGlyphs() not working in 126
        StringBuilder out = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            int c = str.charAt(i);
            if (c >= 0xE000 && c <= 0xF8FF) {
                continue;
            }
            out.append((char) c);
        }
        return out.toString();
    }

    public String longToTime(long seconds) {
        long min = seconds / 60;
        long hour = min / 60;
        return String.format("%d:%02d:%02d", hour, min % 60, seconds % 60);
    }

    public String translateChat(Player player, Player otherPlayer, String message) {
        String locale = otherPlayer.locale;
        try {
            locale = DBHandler.get(otherPlayer.uuid(), Users.TRANSLATOR);
        } catch (Throwable t) {
            Log.err(t);
            DiscordLogger.err(t);
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
        String playerName = player.coloredName();

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

    protected static Locale parseLocale(String code) {
        if (code.contains("_")) {
            String[] codes = code.split("_");
            return new Locale(codes[0], codes[1]);
        }
        return new Locale(code);
    }

    public static Locale findLocale(String code) {
        Locale locale = Structs.find(Const.supportedLocales, l -> l.toString().equals(code) ||
                code.startsWith(l.toString()));
        return locale != null ? locale : Const.defaultLocale();
    }
}
