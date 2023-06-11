package stellar.components;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Strings;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.entities.Effect;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Packets;
import stellar.Const;
import stellar.Variables;
import stellar.bot.Bot;
import stellar.bot.Colors;
import stellar.bot.Util;
import stellar.database.Database;
import stellar.database.enums.PlayerEventTypes;
import stellar.database.enums.ServerEventTypes;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.PlayerEventsRecord;
import stellar.database.gen.tables.records.PlaytimeRecord;
import stellar.database.gen.tables.records.ServerEventsRecord;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.util.Bundle;
import stellar.util.Players;
import stellar.util.StringUtils;
import stellar.util.Translator;
import stellar.util.logger.DiscordLogger;
import types.AdminActionEntry;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;

import static mindustry.Vars.*;
import static stellar.Variables.*;

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
                if (Database.playerExists(event.player)) {
                    if (Database.isBanned(event.player)) {
                        event.player.kick(Packets.KickReason.banned);
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
                    { Bundle.get("welcome.close", locale) },
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

                if (Database.playerExists(event.player)) {
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
                    if (data.getDonated() > 0) {
                        Variables.donaters.put(event.player.uuid(), event.player.name);
                    }
                    if (data.getPopup() == 1) {
                        Call.menu(event.player.con(), 0, title, welcome, buttons); // TODO: enum of menus and buttons
                    } else if (data.getDiscord() == 1) {
                        Call.openURI(event.player.con(), config.discordUrl);
                    }

                } else {
                    UsersRecord usersRecord = Database.getContext().newRecord(Tables.USERS);
                    usersRecord.setUuid(event.player.uuid());
                    usersRecord.setIp(event.player.ip());
                    usersRecord.setName(event.player.name());
                    usersRecord.setLocale(event.player.locale());
                    usersRecord.setAdmin((byte) (event.player.admin() ? 1 : 0));
                    usersRecord.store();

                    PlaytimeRecord playtimeRecord = Database.getContext().newRecord(Tables.PLAYTIME);
                    playtimeRecord.setUuid(event.player.uuid());
                    playtimeRecord.store();

                    Call.menu(event.player.con(), 0, title, welcome, buttons);
                }
            } catch (SQLException e) {
                Log.err(e);
                DiscordLogger.err(e);
                Call.menu(event.player.con(), 0, title, welcome, buttons);

            }
        });
        // endregion

        Events.on(EventType.MenuOptionChooseEvent.class, event -> {
            Log.debug("Menu &lb#@&fr: @", event.menuId, event.option);
            if (event.menuId == 0) {
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
            } else {
                if (!admins.containsKey(event.player.uuid())) {
                    return;
                }

                if (!adminActions.containsKey(event.menuId)) {
                    Log.err("@ tried to access non-existent admin action with ID @", event.player.plainName(), event.menuId);
                    return;
                }

                AdminActionEntry actionEntry = adminActions.get(event.menuId);
                int computed = actionEntry.getUntil();
                switch (event.option) {
                    case 0 -> computed -= 1;
                    case 1 -> computed += 1;
                    case 2 -> computed -= 7;
                    case 3 -> computed += 7;
                    case 4 -> computed -= 30;
                    case 5 -> computed += 30;
                    case 6 -> computed = 0;
                    case 7 -> computed = -1;
                    case 8 -> { } // being processed later
                }

                if (event.option != 7 && event.option != 8) {
                    computed = Math.max(0, computed);
                }
                adminActions.get(event.menuId).setUntil(computed);
                String[][] buttons = {
                        {"-1D", "+1D"},
                        {"-1W", "+1W"},
                        {"-1M", "+1M"},
                        {"[teal]Reset[]", "[red]Permanent[]"},
                        {"[scarlet]Ban![]"}
                }; // TODO: locales, cancel

                if (event.option != 8 && event.option != -1) {
                    Call.followUpMenu(event.menuId, "Period", "Current period: [accent]" + computed + " days[].", buttons);
                } else {
                    UsersRecord adminInfo = actionEntry.getAdmin();
                    UsersRecord targetInfo = actionEntry.getTarget();
                    String adminName = Strings.stripColors(adminInfo.getName());
                    String targetName = Strings.stripColors(targetInfo.getName());
                    String reason = actionEntry.getReason();
                    Log.debug("@ > @ | @ | @ days", adminName, targetName, reason, computed);
                    Call.hideFollowUpMenu(event.menuId);

                    String title, message;
                    if (actionEntry.getAction() == Packets.AdminAction.kick) {
                        title = "Кик";
                        message = """
                                **Админ**: %admin% (%aid%)
                                **Нарушитель**: %target% (%tid%)
                                **Причина**: %reason%
                                """.replace("%admin%", adminName).replace("%aid%", adminInfo.getId().toString())
                                .replace("%target%", targetName).replace("%tid%", targetInfo.getId().toString())
                                .replace("%reason%", actionEntry.getReason());
                    } else {
                        title = "Бан";
                        message = """
                                **Админ**: %admin% (%aid%)
                                **Нарушитель**: %target% (%tid%)
                                **Причина**: %reason%
                                **Срок**: <t:%timestamp%:f>
                                """.replace("%admin%", adminName).replace("%aid%", adminInfo.getId().toString())
                                .replace("%target%", targetName).replace("%tid%", targetInfo.getId().toString())
                                .replace("%reason%", actionEntry.getReason())
                                .replace("%timestamp%", (System.currentTimeMillis() / 1000 + computed * (24 * 60 * 60)) + "");

                    }
                    Bot.sendEmbed(Util.embedBuilder(title, message, Colors.purple, LocalDateTime.now(), Const.SERVER_COLUMN_NAME));
                    try {
                        actionEntry.storeRecord();
                    } catch (SQLException e) {
                        Log.err(e);
                    }
                    netServer.admins.unbanPlayerID(actionEntry.getTarget().getUuid()); // maybe not a good idea
                    adminActions.remove(event.menuId);
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
                String reason = actionEntry.getReason();

                String title = "Кик";
                String message = """
                                **Админ**: %admin% (%aid%)
                                **Нарушитель**: %target% (%tid%)
                                **Причина**: %reason%
                                """.replace("%admin%", adminName).replace("%aid%", adminInfo.getId().toString())
                        .replace("%target%", targetName).replace("%tid%", targetInfo.getId().toString())
                        .replace("%reason%", actionEntry.getReason());
                Bot.sendEmbed(Util.embedBuilder(title, message, Colors.purple, LocalDateTime.now(), Const.SERVER_COLUMN_NAME));
            } else {
                String[][] buttons = {
                        {"-1D", "+1D"},
                        {"-1W", "+1W"},
                        {"-1M", "+1M"},
                        {"[teal]Reset[]", "[red]Permanent[]"},
                        {"[scarlet]Ban![]"}
                }; // TODO: locales
                Call.followUpMenu(event.player.con(), event.textInputId, "Period", "Current period: [accent]" + adminActions.get(event.textInputId).getUntil() + " days[].", buttons);
            }
        });

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
                        Call.textInput(event.player.con(), id, "Reason", "Reason", 64, "", false);
                    }
                } catch (SQLException e) {
                    Log.err(e);
                }
            } else {
                player.sendMessage("[scarlet]?![]");
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
                DiscordLogger.warn(String.format("%s положил торий в реактор (%f, %f)", player.name, event.tile.x, event.tile.y));
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
                    record.setType(PlayerEventTypes.KICK.name());
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
            /*if (!donaters.containsKey(event.player.uuid())) {
                return;
            }*/
            String effectName = Core.settings.getString("effect", "burning"); // TODO: перенести в конфиг файл
            Effect effect = Fx.burning;
            try {
                effect = (Effect) Fx.class.getField(effectName).get(effect);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                Log.err(e);
            }
            Effect finalEffect = effect;
            Groups.player.each(p -> {
                if (p.unit().moving()) {
                    Call.effect(finalEffect, p.x, p.y, 0, Color.white);
                }
            });
        });
    }
}
