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
import stellar.database.PlayerData;
import stellar.Variables;
import stellar.database.DBHandler;
import stellar.database.tables.Users;
import stellar.history.struct.CacheSeq;
import stellar.util.Bundle;
import stellar.util.Translator;
import stellar.util.logger.DiscordLogger;

import java.sql.SQLException;
import java.util.Locale;

import static mindustry.Vars.*;
import static mindustry.Vars.world;
import static stellar.Variables.*;

@SuppressWarnings({"unused", "unchecked"})
public class EventHandler {
    public static void load() {
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
                if (DBHandler.userExist(event.player.uuid())) {
                    DBHandler.update(event.player.uuid(), Users.NAME, event.player.name);
                    DBHandler.update(event.player.uuid(), Users.LOCALE, event.player.locale);
                    DBHandler.update(event.player.uuid(), Users.IP, event.player.ip());

                    PlayerData data = DBHandler.get(event.player.uuid());

                    assert data != null;
                    if (data.isAdmin()) {
                        Variables.admins.put(event.player.uuid(), event.player.name);
                        event.player.admin = true;
                    }
                    if (data.isJsallowed()) {
                        Variables.jsallowed.put(event.player.uuid(), event.player.name);
                    }
                    if (data.getDonated() > 0) {
                        Variables.donaters.put(event.player.uuid(), event.player.name);
                    }
                } else {
                    PlayerData data = PlayerData.builder()
                            .uuid(event.player.uuid())
                            .ip(event.player.ip())
                            .name(event.player.name())
                            .locale(event.player.locale())
                            .admin(event.player.admin())
                            .build();

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
                Log.err("Failed to ban uuid for player '@'", event.uuid);
                Log.err(e);
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
                Log.err("Failed to ban ip for player '@'", event.ip);
                Log.err(e);
                DiscordLogger.err("Failed to ban ip for player '" + uuid + "'", e);
            }
        });

        Events.on(EventType.PlayerUnbanEvent.class, event -> {
            try {
                DBHandler.update(event.uuid, Users.BANNED, false);
            } catch (SQLException e) {
                Log.err("Failed to unban uuid for player '@'", event.uuid);
                Log.err(e);
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
                Log.err("Failed to unban ip for player '@'", uuid);
                Log.err(e);
                DiscordLogger.err("Failed to unban ip for player '" + uuid + "'", e);
            }
        });
        // endregion

        // region отключение
        Events.on(EventType.PlayerLeave.class, event -> {
            if (Variables.votesRTV.contains(event.player.uuid())) {
                Variables.votesRTV.remove(event.player.uuid());
                int cur = Variables.votesRTV.size();
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
        });
        // endregion

        Events.on(EventType.ServerLoadEvent.class, event -> {
            Log.info("ThedimasPlugin: Server loaded");
            Core.settings.put("playerlimit", 69);
        });

        Events.on(EventType.GameOverEvent.class, event -> Variables.votesRTV.clear());

        // region ториевые реакторы
        Events.on(EventType.DepositEvent.class, event -> {
            Player target = event.player;
            Building building = event.tile;

            if (building.block() == Blocks.thoriumReactor && event.item == Items.thorium
                    && target.team().cores().contains(c -> event.tile.dst(c.x, c.y) < 300)) {
                String playerName = event.player.coloredName();
                Bundle.bundled("events.deposit.thorium-in-reactor", playerName, building.tileX(), building.tileY());

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
                    Bundle.bundled("events.build-select.reactor-near-core", playerName, event.tile.x, event.tile.y);

                    Log.info("@ начал строить ториевый реактор близко к ядру (@, @)", player.name, event.tile.x, event.tile.y);
                    DiscordLogger.warn(String.format("%s начал строить ториевый реактор близко к ядру (%d, %d)", player.name, event.tile.x, event.tile.y));
                }
            }
        });
        // endregion

        Events.on(EventType.WorldLoadEvent.class, event -> {
            Variables.history = new CacheSeq[world.width()][world.height()];
        });

        Events.on(EventType.PlayerChatEvent.class, event -> {
            if (!event.message.startsWith("/")) {
                Groups.player.each(otherPlayer -> {
                    String msg = Translator.translateChat(event.player, otherPlayer, event.message);
                    otherPlayer.sendMessage(msg);
                });

                Log.info(Const.CHAT_LOG_FORMAT, Strings.stripColors(event.player.name), Strings.stripColors(event.message), event.player.locale);
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
