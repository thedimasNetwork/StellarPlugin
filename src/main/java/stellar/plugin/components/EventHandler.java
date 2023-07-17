package stellar.plugin.components;

import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.entities.Effect;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Packets;
import org.jooq.Field;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.bot.Bot;
import stellar.plugin.bot.Colors;
import stellar.plugin.bot.Util;
import stellar.database.Database;
import stellar.database.enums.PlayerEventTypes;
import stellar.database.enums.ServerEventTypes;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.*;
import stellar.plugin.enums.Menus;
import stellar.plugin.menus.MenuHandler;
import stellar.plugin.types.AdminActionEntry;
import stellar.plugin.util.Bundle;
import stellar.plugin.util.Players;
import stellar.plugin.util.Translator;
import stellar.plugin.util.logger.DiscordLogger;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;

import static stellar.plugin.Variables.*;
import static stellar.plugin.util.StringUtils.longToTime;
import static stellar.plugin.util.StringUtils.targetColor;

@SuppressWarnings({"unused", "unchecked"})
public class EventHandler {
    public static void load() {
        // region PlayerConnect
        Events.on(EventType.PlayerConnect.class, event -> {
            if (Players.isBot(event.player)) {
                return;
            }
            String uuid = event.player.uuid();
            String name = event.player.name;
            try {
                if (Database.playerExists(event.player.uuid())) {
                    if (Database.isBanned(event.player.uuid())) {
                        Locale locale = Bundle.findLocale(event.player.locale());
                        BansRecord record = Database.latestBan(event.player.uuid());
                        UsersRecord admin = Database.getPlayer(record.getAdmin());
                        String adminName = admin != null ? Strings.stripColors(admin.getName()) : record.getAdmin();
                        String banDate = record.getCreated().format(Const.DATE_FORMATTER);
                        String unbanDate = record.getUntil() != null ? record.getUntil().format(Const.DATE_FORMATTER) : Bundle.get("events.join.banned.forever", locale);

                        String message = Bundle.format("events.join.banned", locale, adminName, banDate, record.getReason().strip(), unbanDate, config.discordUrl);
                        event.player.kick(message);
                    }
                }
            } catch (SQLException e) {
                Log.err(e);
                DiscordLogger.err(e);
            }

            for (String pirate : Const.PIRATES) {
                if (name.toLowerCase().contains(pirate)) {
                    event.player.con.kick(Bundle.get("events.join.player-pirate", Bundle.findLocale(event.player.locale)));
                    break;
                }
            }
        });
        // endregion

        // region PlayerJoin
        Events.on(EventType.PlayerJoin.class, event -> {
            Log.info(Const.JOIN_LOG_FORMAT, event.player.name, event.player.locale, event.player.con.address);
            String playerName = event.player.coloredName();
            Bundle.bundled("events.join.player-join", playerName);

            Locale locale = Bundle.findLocale(event.player.locale);
            String rules = Bundle.get("rules", locale);
            String welcome = Bundle.format("welcome", locale, rules);
            String title = Bundle.format("welcome.title", locale, Strings.stripColors(event.player.name()));
            String[][] buttons = {
                    { Bundle.get("menus.close", locale) },
                    { Bundle.get("welcome.discord", locale) },
                    { Bundle.get("welcome.disable", locale) }
            };

            try {
                PlayerEventsRecord record = Database.getContext().newRecord(Tables.PLAYER_EVENTS);
                record.setServer(Const.SERVER_COLUMN_NAME);
                record.setTimestamp(System.currentTimeMillis() / 1000);
                record.setType(PlayerEventTypes.JOIN.name());
                record.setName(event.player.name());
                record.setUuid(event.player.uuid());
                record.setIp(event.player.ip());
                record.store();

                if (Database.playerExists(event.player.uuid())) {
                    Database.getContext()
                            .update(Tables.USERS)
                            .set(Tables.USERS.NAME, event.player.name())
                            .set(Tables.USERS.LOCALE, event.player.locale())
                            .set(Tables.USERS.IP, event.player.ip())
                            .where(Tables.USERS.UUID.eq(event.player.uuid()))
                            .execute();

                    UsersRecord data = Database.getContext()
                            .selectFrom(Tables.USERS)
                            .where(Tables.USERS.UUID.eq(event.player.uuid()))
                            .fetchOne();

                    assert data != null;
                    if (data.getAdmin() == 1) {
                        Variables.admins.put(event.player.uuid(), event.player.name);
                        event.player.admin = true;
                    }
                    if (data.getJsallowed() == 1) {
                        Variables.jsallowed.put(event.player.uuid(), event.player.name);
                    }
                    //  if (data.getDonated() > 0) {
                    //      Variables.donaters.put(event.player.uuid(), event.player.name);
                    //  }
                    if (data.getPopup() == 1) {
                        Call.menu(event.player.con(), Menus.welcome.ordinal(), title, welcome, buttons); // TODO: enum of menu buttons
                    } else if (data.getDiscord() == 1) {
                        Call.openURI(event.player.con(), config.discordUrl);
                    }

                } else {
                    UsersRecord usersRecord = Database.getContext().newRecord(Tables.USERS); // TODO: Database.createPlayer
                    usersRecord.setUuid(event.player.uuid());
                    usersRecord.setIp(event.player.ip());
                    usersRecord.setName(event.player.name());
                    usersRecord.setLocale(event.player.locale());
                    usersRecord.setAdmin((byte) (event.player.admin() ? 1 : 0));
                    usersRecord.store();

                    PlaytimeRecord playtimeRecord = Database.getContext().newRecord(Tables.PLAYTIME);
                    playtimeRecord.setUuid(event.player.uuid());
                    playtimeRecord.store();

                    Call.menu(event.player.con(), Menus.welcome.ordinal(), title, welcome, buttons);
                }

                if (statsData.containsKey(event.player.uuid())) {
                    Players.incrementStats(event.player, "logins");
                } else {
                    statsData.put(event.player.uuid(), ObjectMap.of(
                            "attacks", 0,
                            "waves", 0,
                            "built", 0,
                            "broken", 0,
                            "deaths", 0,
                            "logins", 1,
                            "messages", 0
                    ));
                }

                Rank.getRank(event.player);
            } catch (SQLException e) {
                Log.err(e);
                DiscordLogger.err(e);
                Call.menu(event.player.con(), Menus.welcome.ordinal(), title, welcome, buttons);
                ranks.put(event.player.uuid(), Rank.player);
            }
        });
        // endregion

        Events.on(EventType.MenuOptionChooseEvent.class, event -> {
            Log.debug("Menu &lb#@&fr: @", event.menuId, event.option);

            if (event.menuId > Menus.values().length - 1) {
                MenuHandler.handle(event.menuId, event.option, event.player);
                return;
            }

            switch (Menus.values()[event.menuId]) {
                case welcome ->  {
                    switch (event.option) {
                        case 0 -> {
                            // do nothing, it's close button
                        }
                        case 1 -> {
                            Call.openURI(event.player.con(), config.discordUrl);
                        }
                        case 2 -> {
                            try {
                                Database.getContext()
                                        .update(Tables.USERS)
                                        .set(Tables.USERS.POPUP, (byte) 0)
                                        .where(Tables.USERS.UUID.eq(event.player.uuid()))
                                        .execute();
                                Bundle.bundled(event.player, "welcome.disabled");
                            } catch (SQLException e) {
                                Log.err(e);
                                Bundle.bundled(event.player, "welcome.disable.failed");
                            }
                        }
                    }
                }
                case punishment -> {
                    if (!admins.containsKey(event.player.uuid())) {
                        return;
                    }

                    if (!adminActions.containsKey(event.menuId)) {
                        Log.err("@ tried to access non-existent admin action with ID @", event.player.plainName(), event.menuId);
                        return;
                    }

                    AdminActionEntry actionEntry = adminActions.get(event.menuId);
                    int computed = actionEntry.getPeriod();
                    switch (event.option) {
                        case 0 -> computed -= 1;
                        case 1 -> computed += 1;
                        case 2 -> computed -= 7;
                        case 3 -> computed += 7;
                        case 4 -> computed -= 30;
                        case 5 -> computed += 30;
                        case 6 -> computed = 0;
                        case 7 -> computed = -1;
                        case 8 -> {} // being processed later
                    }

                    if (event.option != 7 && event.option != 8) {
                        computed = Math.max(0, computed);
                    }
                    adminActions.get(event.menuId).setPeriod(computed);

                    Locale locale = Bundle.findLocale(event.player.locale());

                    if (event.option != 8 && event.option != -1) {
                        String[][] buttons = {
                                {Bundle.get("menus.ban.minus-1d", locale), Bundle.get("menus.ban.plus-1d", locale)},
                                {Bundle.get("menus.ban.minus-1w", locale), Bundle.get("menus.ban.plus-1w", locale)},
                                {Bundle.get("menus.ban.minus-1m", locale), Bundle.get("menus.ban.plus-1m", locale)},
                                {Bundle.get("menus.ban.reset", locale), Bundle.get("menus.ban.permanent", locale)},
                                {Bundle.get("menus.ban.proceed", locale)}
                        };

                        String title = Bundle.format("menus.ban.title", locale, Strings.stripColors(actionEntry.getTarget().getName()));
                        String message = Bundle.format("menus.ban.msg", locale, computed);
                        Call.followUpMenu(event.player.con(), event.menuId, title, message, buttons);
                    } else {
                        UsersRecord adminInfo = actionEntry.getAdmin();
                        UsersRecord targetInfo = actionEntry.getTarget();
                        String adminName = Strings.stripColors(adminInfo.getName());
                        String targetName = Strings.stripColors(targetInfo.getName());
                        Call.hideFollowUpMenu(event.menuId);

                        String title, message;
                        java.awt.Color color;
                        if (actionEntry.getAction() == Packets.AdminAction.kick) {
                            title = "Кик";
                            message = """
                                    **Админ**: %admin% (%aid%)
                                    **Нарушитель**: %target% (%tid%)
                                    **Причина**: %reason%
                                    """.replace("%admin%", adminName).replace("%aid%", adminInfo.getId().toString())
                                    .replace("%target%", targetName).replace("%tid%", targetInfo.getId().toString())
                                    .replace("%reason%", actionEntry.getReason().strip());
                            color = Colors.purple;
                        } else {
                            title = "Бан";
                            message = """
                                    **Админ**: %admin% (%aid%)
                                    **Нарушитель**: %target% (%tid%)
                                    **Причина**: %reason%
                                    """.replace("%admin%", adminName).replace("%aid%", adminInfo.getId().toString())
                                    .replace("%target%", targetName).replace("%tid%", targetInfo.getId().toString())
                                    .replace("%reason%", actionEntry.getReason().strip());
                            color = Colors.red;
                            if (computed > -1) {
                                message += "**Срок**: <t:%timestamp%:f>".replace("%timestamp%", (System.currentTimeMillis() / 1000 + computed * (24 * 60 * 60)) + "");
                            } else {
                                message += "**Срок**: Перманентный";
                            }

                        }
                        Bot.sendEmbed(config.bot.bansId, Util.embedBuilder(title, message, color, LocalDateTime.now(), Const.SERVER_COLUMN_NAME));
                        try {
                            actionEntry.storeRecord();
                        } catch (SQLException e) {
                            Log.err(e);
                        }
                        Vars.netServer.admins.unbanPlayerID(actionEntry.getTarget().getUuid()); // maybe not a good idea
                        adminActions.remove(event.menuId);
                    }
                }
                case ranks -> {
                    switch (event.option) { // TODO: menu instead of infoMessage
                        case 0 -> {
                            try {
                                Rank rank = Rank.getRank(event.player).getNext();
                                Locale locale = Bundle.findLocale(event.player.locale());

                                if (rank == null) {
                                    Call.infoMessage(event.player.con(), Bundle.format("commands.rank.next-rank.none", locale));
                                } else {
                                    UsersRecord record = Database.getPlayer(event.player.uuid());
                                    int playtime = (int) Players.totalPlaytime(event.player.uuid());
                                    String rankStr = rank.icon != null ?
                                            String.format("<[#%s]%s[]> %s", rank.color, rank.icon, Bundle.get("ranks." + rank.name(), locale)) :
                                            Bundle.get("ranks." + rank.name(), locale);

                                    String message = Bundle.format("commands.rank.next-rank.info", locale,
                                            rankStr,
                                            targetColor(record.getAttacks(), rank.requirements.attacks), record.getAttacks(), rank.requirements.attacks,
                                            targetColor(record.getWaves(), rank.requirements.waves), record.getWaves(), rank.requirements.waves,
                                            targetColor(record.getHexes(), rank.requirements.hexes), record.getHexes(), rank.requirements.hexes,
                                            targetColor(record.getBuilt(), rank.requirements.built), record.getBuilt(), rank.requirements.built,
                                            targetColor(playtime, rank.requirements.playtime * 60), longToTime(playtime, locale), longToTime(rank.requirements.playtime * 60, locale)
                                    );
                                    Call.infoMessage(event.player.con(), message);
                                }
                            } catch (SQLException e) {
                                Log.err(e);
                                Bundle.bundled(event.player, "commands.rank.error");
                            }
                        }
                        case 1 -> {
                            // do nothing as menu is being hidden automatically
                        }
                    }
                }
            }
        });

        Events.on(EventType.TextInputEvent.class, event -> {
            if (!admins.containsKey(event.player.uuid())) {
                return;
            }

            Log.debug("@: @ (@)", event.player.plainName(), event.text, event.textInputId);
            if (!adminActions.containsKey(event.textInputId)) {
                Log.err("@ tried to access non-existent admin action with ID @", event.player.plainName(), event.textInputId);
                return;
            }

            AdminActionEntry actionEntry = adminActions.get(event.textInputId);
            if (event.text == null || event.text.isBlank()) {
                actionEntry.setReason("<не указана>");
            } else {
                actionEntry.setReason(event.text);
            }

            if (actionEntry.getAction() == Packets.AdminAction.kick) {
                UsersRecord adminInfo = actionEntry.getAdmin();
                UsersRecord targetInfo = actionEntry.getTarget();
                String adminName = Strings.stripColors(adminInfo.getName());
                String targetName = Strings.stripColors(targetInfo.getName());

                String title = "Кик";
                String message = """
                                **Админ**: %admin% (%aid%)
                                **Нарушитель**: %target% (%tid%)
                                **Причина**: %reason%
                                """.replace("%admin%", adminName).replace("%aid%", adminInfo.getId().toString())
                        .replace("%target%", targetName).replace("%tid%", targetInfo.getId().toString())
                        .replace("%reason%", actionEntry.getReason().strip());
                Bot.sendEmbed(config.bot.bansId, Util.embedBuilder(title, message, Colors.purple, LocalDateTime.now(), Const.SERVER_COLUMN_NAME));
            } else {
                Locale locale = Bundle.findLocale(event.player.locale());
                String[][] buttons = {
                        {Bundle.get("menus.ban.minus-1d", locale), Bundle.get("menus.ban.plus-1d", locale)},
                        {Bundle.get("menus.ban.minus-1w", locale), Bundle.get("menus.ban.plus-1w", locale)},
                        {Bundle.get("menus.ban.minus-1m", locale), Bundle.get("menus.ban.plus-1m", locale)},
                        {Bundle.get("menus.ban.reset", locale), Bundle.get("menus.ban.permanent", locale)},
                        {Bundle.get("menus.ban.proceed", locale)}
                };
                String title = Bundle.format("menus.ban.title", locale, Strings.stripColors(actionEntry.getTarget().getName()));
                String message = Bundle.format("menus.ban.msg", locale, actionEntry.getPeriod());
                Call.followUpMenu(event.player.con(), event.textInputId, title, message, buttons);
            }
        });

        /*
        // region баны
        Events.on(EventType.PlayerBanEvent.class, event -> {
            try {
                Database.getContext()
                        .update(Tables.USERS)
                        .set(Tables.USERS.BANNED, (byte) 1)
                        .where(Tables.USERS.UUID.eq(event.uuid))
                        .execute();
            } catch (SQLException e) {
                Log.err("Failed to ban uuid for player '@'", event.uuid);
                Log.err(e);
                DiscordLogger.err("Failed to ban uuid for player '" + event.uuid + "'", e);
            }
        });

        // I just deleted IP ban/unban part, haha
        // It isn't used anyway

        Events.on(EventType.PlayerUnbanEvent.class, event -> {
            try {
                Database.getContext()
                        .update(Tables.USERS)
                        .set(Tables.USERS.BANNED, (byte) 0)
                        .where(Tables.USERS.UUID.eq(event.uuid))
                        .execute();
            } catch (SQLException e) {
                Log.err("Failed to unban uuid for player '@'", event.uuid);
                Log.err(e);
                DiscordLogger.err("Failed to unban uuid for player '" + event.uuid + "'", e);
            }
        });
        */

        Events.on(EventType.AdminRequestEvent.class, event -> {
            if (admins.containsKey(event.player.uuid())) {
                try {
                    ServerEventsRecord record = Database.getContext().newRecord(Tables.SERVER_EVENTS);
                    record.setServer(Const.SERVER_COLUMN_NAME);
                    record.setTimestamp(System.currentTimeMillis() / 1000);
                    record.setType(ServerEventTypes.ADMIN_REQUEST.name());
                    record.setName(event.player.name);
                    record.setUuid(event.player.uuid());
                    record.setIp(event.player.ip());
                    record.setRequest(event.action.name());
                    record.store();

                    if (event.action == Packets.AdminAction.kick || event.action == Packets.AdminAction.ban) {
                        PlayerEventsRecord record2 = Database.getContext().newRecord(Tables.PLAYER_EVENTS);
                        record2.setServer(Const.SERVER_COLUMN_NAME);
                        record2.setTimestamp(System.currentTimeMillis() / 1000);
                        record2.setType(event.action == Packets.AdminAction.kick ? PlayerEventTypes.KICK.name() : PlayerEventTypes.BAN.name());
                        record2.setName(event.other.name);
                        record2.setUuid(event.other.uuid());
                        record2.setIp(event.other.ip());
                        record2.store();

                        int id = Mathf.random(0, Integer.MAX_VALUE - 1);
                        UsersRecord adminInfo = Database.getContext()
                                .selectFrom(Tables.USERS)
                                .where(Tables.USERS.UUID.eq(event.player.uuid()))
                                .fetchOne();
                        UsersRecord targetInfo = Database.getContext()
                                .selectFrom(Tables.USERS)
                                .where(Tables.USERS.UUID.eq(event.other.uuid()))
                                .fetchOne();

                        AdminActionEntry entry = new AdminActionEntry(adminInfo, targetInfo, event.action);
                        adminActions.put(id, entry);
                        Locale locale = Bundle.findLocale(event.player.locale());
                        String title = Bundle.format("inputs.punishment.title", locale, event.other.plainName());
                        String message = Bundle.get("inputs.punishment.msg", locale);
                        Call.textInput(event.player.con(), id, title, message, 64, "", false);
                    }
                } catch (SQLException e) {
                    Log.err(e);
                }
            } else {
                event.player.sendMessage("[scarlet]?![]");
            }
        });
        // endregion

        // region отключение
        Events.on(EventType.PlayerLeave.class, event -> {
            if (Variables.votesRTV.contains(event.player.uuid())) {
                Variables.votesRTV.remove(event.player.uuid());
                int cur = Variables.votesRTV.size;
                int req = (int) Math.ceil(Const.VOTES_RATIO * Groups.player.size());
                String playerName = event.player.coloredName();
                Bundle.bundled("commands.rtv.leave", playerName, cur, req);
            }

            Variables.admins.remove(event.player.uuid());
            Variables.jsallowed.remove(event.player.uuid());
            Variables.donaters.remove(event.player.uuid());
            Variables.activeHistoryPlayers.remove(event.player.uuid());
            Log.info("@ has disconnected from the server", event.player.name);
            String playerName = event.player.coloredName();
            Bundle.bundled("events.leave.player-leave", playerName);

            try {
                PlayerEventsRecord record = Database.getContext().newRecord(Tables.PLAYER_EVENTS);
                record.setServer(Const.SERVER_COLUMN_NAME);
                record.setTimestamp(System.currentTimeMillis() / 1000);
                record.setType(PlayerEventTypes.LEAVE.name());
                record.setName(event.player.name());
                record.setUuid(event.player.uuid());
                record.setIp(event.player.ip());
                record.store();
            } catch (SQLException e) {
                Log.err(e);
            }

            unitPlayer.remove(event.player.unit().id);
            ranks.remove(event.player.uuid());
        });
        // endregion

        Events.on(EventType.ServerLoadEvent.class, event -> {
            Log.info("ThedimasPlugin: Server loaded");
            try {
                ServerEventsRecord record = Database.getContext().newRecord(Tables.SERVER_EVENTS);
                record.setServer(Const.SERVER_COLUMN_NAME);
                record.setTimestamp(System.currentTimeMillis() / 1000);
                record.setType(ServerEventTypes.START.name());
                record.store();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        Events.on(EventType.GameOverEvent.class, event -> {
            try {
                Variables.votesRTV.clear();
                ServerEventsRecord record = Database.getContext().newRecord(Tables.SERVER_EVENTS);
                record.setServer(Const.SERVER_COLUMN_NAME);
                record.setTimestamp(System.currentTimeMillis() / 1000);
                record.setType(ServerEventTypes.GAMEOVER.name());
                record.store();
            } catch (SQLException e) {
                Log.err(e);
            }
            if (Vars.state.rules.mode() == Gamemode.attack && event.winner == Team.sharded) {
                Groups.player.each(player -> {
                    Players.incrementStats(player, "attacks");
                });
            }
        });

        Events.on(EventType.WaveEvent.class, event -> {
            if (Vars.state.rules.mode() != Gamemode.sandbox) {
                Groups.player.each(player -> {
                    Players.incrementStats(player, "waves");
                });
            }
        });

        Events.on(EventType.BlockBuildEndEvent.class, event -> {
            if (event.tile.build == null) {
                return;
            }

            if (event.unit.isPlayer()) {
                Players.incrementStats(event.unit.getPlayer(), event.breaking ? "broken" : "built");
            }
        });

        Events.on(EventType.UnitDestroyEvent.class, event -> {
            Log.debug("== @ died ==", event.unit.type().name);
            Log.debug("- Player: @", event.unit.getPlayer());
            Log.debug("- Controller: @", event.unit.getControllerName());
            Log.debug("- Last commanded: @", event.unit.lastCommanded());
            Log.debug("== Workaround: ==");
            Log.debug("- Player: @", Groups.player.getByID(unitPlayer.get(event.unit.id())));

            Player player = event.unit.getPlayer();
            if (player == null) {
                player = Groups.player.getByID(unitPlayer.get(event.unit.id()));
            }

            if (player == null) {
                return;
            }

            Log.debug(player);
            Players.incrementStats(player, "deaths");
        });

        // region ториевые реакторы
        Events.on(EventType.DepositEvent.class, event -> {
            Player target = event.player;
            Building building = event.tile;

            if (building.block() == Blocks.thoriumReactor && event.item == Items.thorium
                    && target.team().cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                String playerName = event.player.coloredName();
                Groups.player.each(o -> o.team() == target.team(), p -> {
                    Bundle.bundled(p, "events.deposit.thorium-in-reactor", playerName, building.tileX(), building.tileY());
                });

                Log.info("@ положил торий в реактор (@, @)", target.name, building.tileX(), building.tileY());
                DiscordLogger.warn(String.format("%s положил торий в реактор (%f, %f)", event.player.name, event.tile.x, event.tile.y));
            }
        });

        Events.on(EventType.BuildSelectEvent.class, event -> {
            if (!event.breaking && event.builder != null && event.builder.buildPlan() != null
                    && event.builder.buildPlan().block == Blocks.thoriumReactor && event.builder.isPlayer()
                    && event.team.cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                Player player = event.builder.getPlayer();
                String playerName = player.coloredName();
                if (interval.get(0, 600)) {
                    Groups.player.each(o -> o.team() == player.team(), p -> {
                        Bundle.bundled(p, "events.build-select.reactor-near-core", playerName, event.tile.x, event.tile.y);
                    });

                    Log.info("@ начал строить ториевый реактор близко к ядру (@, @)", player.name, event.tile.x, event.tile.y);
                    DiscordLogger.warn(String.format("%s начал строить ториевый реактор близко к ядру (%d, %d)", player.name, event.tile.x, event.tile.y));
                }
            }
        });
        // endregion

        Events.on(EventType.PlayerChatEvent.class, event -> {
            if (!event.message.startsWith("/")) {
                try {
                    PlayerEventsRecord record = Database.getContext().newRecord(Tables.PLAYER_EVENTS);
                    record.setServer(Const.SERVER_COLUMN_NAME);
                    record.setTimestamp(System.currentTimeMillis() / 1000);
                    record.setType(PlayerEventTypes.CHAT.name());
                    record.setName(event.player.name);
                    record.setUuid(event.player.uuid());
                    record.setIp(event.player.ip());
                    record.setMessage(event.message);
                    record.store();
                } catch (SQLException e) {
                    Log.err(e);
                }

                Groups.player.each(otherPlayer -> {
                    new Thread(() -> {
                        String msg = Translator.translateChat(event.player, otherPlayer, event.message);
                        otherPlayer.sendMessage(msg);
                    }).start();
                });

                Log.info(Const.CHAT_LOG_FORMAT, Strings.stripColors(event.player.name), Strings.stripColors(event.message), event.player.locale);
                Players.incrementStats(event.player, "messages");
            } else {
                try {
                    PlayerEventsRecord record = Database.getContext().newRecord(Tables.PLAYER_EVENTS);
                    record.setServer(Const.SERVER_COLUMN_NAME);
                    record.setTimestamp(System.currentTimeMillis() / 1000);
                    record.setType(PlayerEventTypes.KICK.name());
                    record.setName(event.player.name);
                    record.setUuid(event.player.uuid());
                    record.setIp(event.player.ip());
                    record.setMessage(event.message.replaceAll("^/", ""));
                    record.store();
                } catch (SQLException e) {
                    Log.err(e);
                }
            }
        });

        Events.run(EventType.Trigger.update, () -> {
            Groups.player.each(player -> {
                if (player.unit().id() != -1) {
                    unitPlayer.put(player.unit().id(), player.id());
                }
            });

            if (Variables.interval.get(2, 3600)) {
                Variables.statsData.each((uuid, stats) -> {
                    stats.each((name, value) -> {
                        try {
                            Field<Integer> field = (Field<Integer>) Tables.USERS.field(name);
                            if (field == null) {
                                Log.err("Field @ is null. UUID: @", name, uuid);
                                return;
                            }

                            Database.getContext()
                                    .update(Tables.USERS)
                                    .set(field, field.plus(value))
                                    .where(Tables.USERS.UUID.eq(uuid))
                                    .execute();

                            stats.put(name, 0);
                        } catch (SQLException e) {
                            Log.err(e);
                        }
                    });

                    if (!Groups.player.contains(p -> p.uuid().equals(uuid))) {
                        Variables.statsData.remove(uuid);
                    }
                });
            }
            /*if (!donaters.containsKey(event.player.uuid())) {
                return;
            }*/
            
            Groups.player.each(p -> {
                if (p.unit().moving()) {
                    try {
                        Rank rank = Rank.getRank(p);
                        Effect effect = rank.effect;
                        Color effectColor = rank.effectColor == null ? Color.white : rank.effectColor;
                        Call.effect(effect, p.x, p.y, 0, effectColor);
                    } catch (SQLException ignored) { } // it can't throw as it calls the DB only when a player joins
                }
            });
        });
    }
}
