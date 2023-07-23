package stellar.plugin.components;

import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.entities.Effect;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.net.Packets;
import org.jooq.DMLQuery;
import org.jooq.Field;
import org.jooq.UpdateSetFirstStep;
import org.jooq.UpdateSetMoreStep;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.bot.Bot;
import stellar.plugin.bot.Colors;
import stellar.plugin.bot.Util;
import stellar.database.Database;
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
import static stellar.plugin.util.NetUtils.updateBackground;

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
                        String banDate = record.getCreated().format(Const.dateFotmatter);
                        String unbanDate = record.getUntil() != null ? record.getUntil().format(Const.dateFotmatter) : Bundle.get("events.join.banned.forever", locale);

                        String message = Bundle.format("events.join.banned", locale, adminName, banDate, record.getReason().strip(), unbanDate, config.discordUrl);
                        event.player.kick(message);
                    }
                }
            } catch (SQLException e) {
                Log.err(e);
                DiscordLogger.err(e);
            }

            for (String pirate : Const.pirates) {
                if (name.toLowerCase().contains(pirate)) {
                    event.player.con.kick(Bundle.get("events.join.player-pirate", Bundle.findLocale(event.player.locale)));
                    break;
                }
            }
        });
        // endregion

        // region PlayerJoin
        Events.on(EventType.PlayerJoin.class, event -> {
            Log.info(Const.joinLogFormat, event.player.name, event.player.locale, event.player.con.address);
            String playerName = event.player.coloredName();
            Bundle.bundled("events.join.player-join", playerName);

            Locale locale = Bundle.findLocale(event.player.locale);
            String rules = Bundle.get("rules", locale);

            StringBuilder commands = new StringBuilder();
            Const.usefulCommands.each(name -> {
                CommandHandler.Command command = Vars.netServer.clientCommands.getCommandList().find(cmd -> cmd.text.equals(name));
                commands.append(String.format("[orange]/%s[] %s\n", command.text, Bundle.has(command.description, locale) ? Bundle.get(command.description, locale) : command.description));
            });

            String welcome = Bundle.format("welcome", locale, rules, commands.toString().strip());
            String title = Bundle.format("welcome.title", locale, Strings.stripColors(event.player.name()));
            String[][] buttons = {
                    { Bundle.get("menus.close", locale) },
                    { Bundle.get("welcome.discord", locale) },
                    { Bundle.get("welcome.disable", locale) }
            };

            try {
                if (Database.playerExists(event.player.uuid())) {
                    updateBackground(Database.getContext() // TODO: Database.updateUser
                            .update(Tables.users)
                            .set(Tables.users.name, event.player.name())
                            .set(Tables.users.locale, event.player.locale())
                            .set(Tables.users.ip, event.player.ip())
                            .where(Tables.users.uuid.eq(event.player.uuid())));

                    UsersRecord data = Database.getPlayer(event.player.uuid());
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
                    if (data.isPopup()) {
                        Call.menu(event.player.con(), Menus.welcome.ordinal(), title, welcome, buttons); // TODO: maybe migrate to MenuHandler
                    } else if (data.isDiscord()) {
                        Call.openURI(event.player.con(), config.discordUrl);
                    }

                } else {
                    Database.createFullPlayer(event.player.uuid(), event.player.ip(), event.player.name(), event.player.locale(), event.player.admin());
                    Call.menu(event.player.con(), Menus.welcome.ordinal(), title, welcome, buttons);
                }

                Players.incrementStats(event.player, "logins");
                
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
                if (MenuHandler.handle(event.menuId, event.option, event.player)) {
                    return;
                }
            }

            if (adminActions.containsKey(event.menuId)) {
                if (!admins.containsKey(event.player.uuid())) {
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
                    case 8 -> {
                    } // being processed later
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
                    Bot.sendEmbed(config.bot.bansId, Util.embedBuilder(title, message, color, LocalDateTime.now(), Const.serverFieldName));
                    try {
                        actionEntry.storeRecord();
                    } catch (SQLException e) {
                        Log.err(e);
                    }
                    Vars.netServer.admins.unbanPlayerID(actionEntry.getTarget().getUuid()); // maybe not a good idea
                    adminActions.remove(event.menuId);
                }
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
                                updateBackground(Database.getContext()
                                        .update(Tables.users)
                                        .set(Tables.users.popup, false)
                                        .where(Tables.users.uuid.eq(event.player.uuid())));
                                Bundle.bundled(event.player, "welcome.disabled");
                            } catch (SQLException e) {
                                Log.err(e);
                                Bundle.bundled(event.player, "welcome.disable.failed");
                            }
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
                Bot.sendEmbed(config.bot.bansId, Util.embedBuilder(title, message, Colors.purple, LocalDateTime.now(), Const.serverFieldName));
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
                        .update(Tables.users)
                        .set(Tables.users.BANNED, (byte) 1)
                        .where(Tables.users.uuid.eq(event.uuid))
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
                        .update(Tables.users)
                        .set(Tables.users.BANNED, (byte) 0)
                        .where(Tables.users.uuid.eq(event.uuid))
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
                    if (event.action == Packets.AdminAction.kick || event.action == Packets.AdminAction.ban) {
                        int id = Mathf.random(0, Integer.MAX_VALUE - 1);
                        UsersRecord adminInfo = Database.getPlayer(event.player.uuid());
                        UsersRecord targetInfo = Database.getPlayer(event.other.uuid());

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
                int req = (int) Math.ceil(Const.votesRatio * Groups.player.size());
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
            unitPlayer.remove(event.player.unit().id);
            ranks.remove(event.player.uuid());
        });
        // endregion

        Events.on(EventType.ServerLoadEvent.class, event -> {
            Log.info("ThedimasPlugin: Server loaded");
        });

        Events.on(EventType.GameOverEvent.class, event -> {
            Variables.votesRTV.clear();
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
                Groups.player.each(otherPlayer -> {
                    new Thread(() -> {
                        String msg = Translator.translateChat(event.player, otherPlayer, event.message);
                        otherPlayer.sendMessage(msg);
                    }).start();
                });

                Log.info(Const.chatLogFormat, Strings.stripColors(event.player.name), Strings.stripColors(event.message), event.player.locale);
                Players.incrementStats(event.player, "messages");
            }
        });

        Events.run(EventType.Trigger.update, () -> {
            Groups.player.each(player -> {
                if (player.unit().id() != -1) {
                    unitPlayer.put(player.unit().id(), player.id());
                }
            });

            if (Variables.interval.get(2, 3600)) {
                new Thread(() -> { // this creates new thread as getting new ranks may also cause some hangs
                    Variables.statsData.each((uuid, stats) -> {
                        try {
                            UpdateSetMoreStep<StatsRecord> query = (UpdateSetMoreStep<StatsRecord>) Database.getContext()
                                    .update(Tables.stats); // omg... looks bad
                            stats.each((name, value) -> {
                                Field<Integer> field = (Field<Integer>) Tables.stats.field(name);
                                if (field == null) {
                                    Log.err("Field @ is null. UUID: @", name, uuid);
                                    return;
                                }
                                query.set(field, field.plus(value));
                                stats.put(name, 0);
                            });
                            query.where(Tables.stats.uuid.eq(uuid))
                                    .execute(); // don't run in the background as rank updating should be on fresh data

                        } catch (SQLException e) {
                            Log.err(e);
                        }

                        if (!Groups.player.contains(p -> p.uuid().equals(uuid))) {
                            Variables.statsData.remove(uuid);
                        }
                    });

                    ObjectMap<String, Rank> oldRanks = ranks.copy();
                    ranks.clear();
                    Groups.player.each(player -> {
                        Rank oldRank = oldRanks.get(player.uuid());
                        Rank newRank = oldRank;

                        try {
                            newRank = Rank.getRank(player);
                        } catch (SQLException e) {
                            Log.err(e);
                            ranks.put(player.uuid(), oldRanks.get(player.uuid()));
                        }

                        if (newRank != oldRank) {
                            Call.warningToast(player.con, Iconc.chartBar, Bundle.format("events.new-rank", Bundle.findLocale(player.locale()), newRank.formatted(player)));
                        }
                    });
                }).start();
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
