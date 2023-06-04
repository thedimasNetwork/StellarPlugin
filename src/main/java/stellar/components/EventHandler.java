package stellar.components;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
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
import stellar.util.Translator;
import stellar.util.logger.DiscordLogger;

import java.sql.SQLException;
import java.util.Locale;

import static mindustry.Vars.*;
import static stellar.Variables.config;
import static stellar.Variables.interval;

@SuppressWarnings({"unused", "unchecked"})
public class EventHandler { // TODO: split into different components
    public static void load() {
        // region PlayerConnect
        Events.on(EventType.PlayerConnect.class, event -> {
            if (Players.isBot(event.player)) {
                return;
            }
            String uuid = event.player.uuid();
            String name = event.player.name;
            try {
                boolean exists = Database.getContext().fetchExists(Tables.USERS, Tables.USERS.UUID.eq(uuid)); // TODO: move to playerExists method
                if (exists) {
                    boolean banned = Database.getContext()
                            .select(Tables.USERS.BANNED)
                            .from(Tables.USERS)
                            .where(Tables.USERS.UUID.eq(uuid))
                            .fetchOne().value1() == 1;
                    if (banned) {
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
            String welcome = Bundle.format("welcome", locale, rules, config.discordUrl);
            Call.infoMessage(event.player.con, welcome);

            try {
                PlayerEventsRecord record = Database.getContext().newRecord(Tables.PLAYER_EVENTS);
                record.setServer(Const.SERVER_COLUMN_NAME);
                record.setTimestamp(System.currentTimeMillis() / 1000);
                record.setType(PlayerEventTypes.JOIN.name());
                record.setName(event.player.name());
                record.setUuid(event.player.uuid());
                record.setIp(event.player.ip());
                record.store();

                boolean exists = Database.getContext().fetchExists(Tables.USERS, Tables.USERS.UUID.eq(event.player.uuid())); // TODO: move to playerExists method
                if (exists) {
                    Database.getContext()
                            .update(Tables.USERS)
                            .set(Tables.USERS.NAME, event.player.name())
                            .where(Tables.USERS.UUID.eq(event.player.uuid()))
                            .execute();

                    Database.getContext()
                            .update(Tables.USERS)
                            .set(Tables.USERS.LOCALE, event.player.locale())
                            .where(Tables.USERS.UUID.eq(event.player.uuid()))
                            .execute();

                    Database.getContext()
                            .update(Tables.USERS)
                            .set(Tables.USERS.IP, event.player.ip())
                            .where(Tables.USERS.UUID.eq(event.player.uuid()))
                            .execute();


                    UsersRecord data = Database.getContext()
                            .selectFrom(Tables.USERS)
                            .where(Tables.USERS.UUID.eq(event.player.uuid()))
                            .fetchOne();

                    assert data != null;
                    if (data.getAdmin() == 1) { // damn, use bools instead of bytes
                        Variables.admins.put(event.player.uuid(), event.player.name);
                        event.player.admin = true;
                    }
                    if (data.getJsallowed() == 1) {
                        Variables.jsallowed.put(event.player.uuid(), event.player.name);
                    }
                    if (data.getDonated() > 0) {
                        Variables.donaters.put(event.player.uuid(), event.player.name);
                    }
                } else {
                    UsersRecord usersRecord = Database.getContext().newRecord(Tables.USERS);
                    usersRecord.setUuid(event.player.uuid());
                    usersRecord.setIp(event.player.ip());
                    usersRecord.setName(event.player.name());
                    usersRecord.setLocale(event.player.locale());
                    usersRecord.setAdmin((byte) (event.player.admin() ? 1 : 0)); // FIXME: that's really awful
                    usersRecord.store();

                    PlaytimeRecord playtimeRecord = Database.getContext().newRecord(Tables.PLAYTIME);
                    playtimeRecord.setUuid(event.player.uuid());
                    playtimeRecord.store();
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

        Events.on(EventType.BlockBuildEndEvent.class, event -> {
            if (event.tile.build == null) {
                return; // игнорируем ломание/строительство блоков по типу валунов
            }

            Player player = event.unit.getPlayer();
            PlayerEventTypes type = PlayerEventTypes.BUILD;
            String blockName = event.tile.build.block.name;
            if (event.breaking) {
                blockName = null;
                type = PlayerEventTypes.BREAK;
            }
            try {
                PlayerEventsRecord record = Database.getContext().newRecord(Tables.PLAYER_EVENTS);
                record.setServer(Const.SERVER_COLUMN_NAME);
                record.setTimestamp(System.currentTimeMillis() / 1000);
                record.setType(type.name());
                record.setUuid(player != null ? player.uuid() : "UNIT_" + event.unit.type().name.toUpperCase());
                record.setName(player != null ? player.name : null);
                record.setIp(player != null ? player.ip() : null);
                record.setX((int) event.tile.x);
                record.setY((int) event.tile.y);
                record.setBlock(blockName);
                record.store();
            } catch (SQLException e) {
                Log.err(e);
            }
        });


        // WorldLoad and Wave events were removed as useless

        Events.on(EventType.AdminRequestEvent.class, event -> {
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

                if (event.action == Packets.AdminAction.kick) {
                    PlayerEventsRecord record2 = Database.getContext().newRecord(Tables.PLAYER_EVENTS);
                    record2.setServer(Const.SERVER_COLUMN_NAME);
                    record2.setTimestamp(System.currentTimeMillis() / 1000);
                    record2.setType(PlayerEventTypes.KICK.name());
                    record2.setName(event.other.name);
                    record2.setUuid(event.other.uuid());
                    record2.setIp(event.other.ip());
                    record2.store();
                } else if (event.action == Packets.AdminAction.ban) {
                    PlayerEventsRecord record2 = Database.getContext().newRecord(Tables.PLAYER_EVENTS);
                    record2.setServer(Const.SERVER_COLUMN_NAME);
                    record2.setTimestamp(System.currentTimeMillis() / 1000);
                    record2.setType(PlayerEventTypes.BAN.name());
                    record2.setName(event.other.name);
                    record2.setUuid(event.other.uuid());
                    record2.setIp(event.other.ip());
                    record2.store();
                }
            } catch (SQLException e) {
                Log.err(e);
            }
        });

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
