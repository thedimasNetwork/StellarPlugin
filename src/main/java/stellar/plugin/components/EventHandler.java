package stellar.plugin.components;

import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.entities.Effect;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.net.Packets;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import stellar.database.DatabaseAsync;
import stellar.database.enums.MessageType;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.plugin.Const;
import stellar.plugin.bot.Bot;
import stellar.plugin.bot.Colors;
import stellar.plugin.bot.Util;
import stellar.plugin.enums.Menus;
import stellar.plugin.history.struct.CacheSeq;
import stellar.plugin.types.AdminActionEntry;
import stellar.plugin.util.Bundle;
import stellar.plugin.util.Players;
import stellar.plugin.util.Translator;
import stellar.plugin.util.logger.DiscordLogger;
import stellar.plugin.util.menus.MenuHandler;

import java.time.LocalDateTime;
import java.util.Locale;

import static stellar.plugin.Variables.*;

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
            DatabaseAsync.playerExistsAsync(
                    uuid
            ).thenComposeAsync(exists -> {
                if (!exists) {
                    throw new IllegalArgumentException("Player does not exist.");
                }
                return DatabaseAsync.isBannedAsync(uuid);
            }).thenComposeAsync(isBanned -> {
                if (!isBanned) {
                    throw new IllegalArgumentException("Player is not banned.");
                }
                return DatabaseAsync.latestBanAsync(
                        uuid
                ).thenCombineAsync(DatabaseAsync.getContextAsync(), (record, context) -> {
                    Locale locale = Bundle.findLocale(event.player.locale());
                    UsersRecord admin = context
                            .selectFrom(Tables.users)
                            .where(Tables.users.uuid.eq(record.getAdmin()))
                            .fetchOne();

                    String adminName = admin != null ? Strings.stripColors(admin.getName()) : record.getAdmin();
                    String banDate = record.getCreated().format(Const.dateFormatter);
                    String unbanDate = record.getUntil() != null ? record.getUntil().format(Const.dateFormatter) : Bundle.get("events.join.banned.forever", locale);

                    String message = Bundle.format("events.join.banned", locale, adminName, banDate, record.getReason().strip(), unbanDate, config.discordUrl);
                    event.player.kick(message);
                    return null;
                });
            }).thenRunAsync(() -> {
                for (String pirate : Const.pirates) {
                    if (name.toLowerCase().contains(pirate)) {
                        event.player.con.kick(Bundle.get("events.join.player-pirate", Bundle.findLocale(event.player.locale)));
                        break;
                    }
                }
            }).exceptionally(t -> {
                if (!(t.getCause() instanceof IllegalArgumentException) && !(t instanceof IllegalArgumentException)) {
                    Log.err(t);
                    DiscordLogger.err(t);
                }
                return null;
            });
        });
        // endregion

        // region PlayerJoin
        Events.on(EventType.PlayerJoin.class, event -> {
            Log.info(Const.joinLogFormat, event.player.name(), event.player.uuid(), event.player.ip(), event.player.locale());
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
                    {Bundle.get("menus.close", locale)},
                    {Bundle.get("welcome.discord", locale)},
                    {Bundle.get("welcome.disable", locale)}
            };

            DatabaseAsync.playerExistsAsync(
                    event.player.uuid()
            ).thenComposeAsync(exists -> {
                Players.incrementStats(event.player, "logins");
                if (exists) {
                    return DatabaseAsync.updatePlayerAsync(
                            event.player.uuid(), event.player.name(), event.player.locale(), event.player.ip()
                    ).thenComposeAsync(ignored ->
                            DatabaseAsync.getPlayerAsync(event.player.uuid())
                    ).thenAcceptAsync(data -> {
                        if (data.getStatus().ordinal() > 0) {
                            admins.put(event.player.uuid(), event.player.name);
                            specialRanks.put(event.player.uuid(), Rank.getSpecialRank(data.getStatus()));
                            event.player.admin = true;
                        }
                        if (data.getDonated() > 0) {
                            donaters.put(event.player.uuid(), event.player.name);
                        }
                        if (data.isPopup()) {
                            Call.menu(event.player.con(), Menus.welcome.ordinal(), title, welcome, buttons); // TODO: maybe migrate to MenuHandler
                        } else if (data.isDiscord()) {
                            Call.openURI(event.player.con(), config.discordUrl);
                        }
                    }).exceptionally(t -> {
                        Log.err(t);
                        DiscordLogger.err(t);
                        return null;
                    });
                } else {
                    return DatabaseAsync.createFullPlayerAsync(
                            event.player.uuid(), event.player.ip(), event.player.name(), event.player.locale(), event.player.admin()
                    ).thenAcceptAsync(ignored -> {
                        Call.menu(event.player.con(), Menus.welcome.ordinal(), title, welcome, buttons);
                    }).thenAcceptAsync(ignored -> {
                        Rank.getRankAsync(event.player);
                    }).exceptionally(t -> {
                        Log.err(t);
                        DiscordLogger.err(t);
                        return null;
                    });
                }
            }).thenComposeAsync(ignored -> DatabaseAsync.createLoginAsync(
                            Const.serverFieldName,
                            event.player.uuid(),
                            event.player.ip(),
                            event.player.name(),
                            event.player.locale())
            ).exceptionally(t -> {
                Log.err(t);
                DiscordLogger.err(t);
                return null;
            });
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
                    actionEntry.storeRecord();
                    Vars.netServer.admins.unbanPlayerID(actionEntry.getTarget().getUuid()); // maybe not a good idea
                    adminActions.remove(event.menuId);
                }
                return;
            }

            if (Menus.values()[event.menuId] == Menus.welcome) {
                switch (event.option) {
                    case 0 -> {
                        // do nothing, it's close button
                    }
                    case 1 -> {
                        Call.openURI(event.player.con(), config.discordUrl);
                    }
                    case 2 -> {
                        DatabaseAsync.getContextAsync().thenComposeAsync(context -> context
                                .update(Tables.users)
                                .set(Tables.users.popup, false)
                                .where(Tables.users.uuid.eq(event.player.uuid()))
                                .executeAsync()
                        ).handle((ignored, t) -> {
                            if (t != null) {
                                Log.err(t);
                                Bundle.bundled(event.player, "welcome.disable.failed");
                            } else {
                                Bundle.bundled(event.player, "welcome.disabled");
                            }
                            return null;
                        }).exceptionally(t -> {
                            Log.err(t);
                            DiscordLogger.err(t);
                            return null;
                        });
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

        Events.on(EventType.AdminRequestEvent.class, event -> {
            if (!admins.containsKey(event.player.uuid())) {
                event.player.sendMessage("[scarlet]?![]");
                return;
            }
            if (!(event.action == Packets.AdminAction.kick || event.action == Packets.AdminAction.ban)) {
                return;
            }

            DatabaseAsync.getPlayerAsync(
                    event.player.uuid()
            ).thenCombineAsync(DatabaseAsync.getPlayerAsync(event.other.uuid()), (adminInfo, targetInfo) -> {
                int id = Mathf.random(0, Integer.MAX_VALUE - 1);
                AdminActionEntry entry = new AdminActionEntry(adminInfo, targetInfo, event.action);
                adminActions.put(id, entry);
                Locale locale = Bundle.findLocale(event.player.locale());
                String title = Bundle.format("inputs.punishment.title", locale, event.other.plainName());
                String message = Bundle.get("inputs.punishment.msg", locale);
                Call.textInput(event.player.con(), id, title, message, 64, "", false);
                return null;
            }).exceptionally(t -> {
                Log.err(t);
                DiscordLogger.err(t);
                return null;
            });
        });
        // endregion

        // region PlayerDisconnect
        Events.on(EventType.PlayerLeave.class, event -> {
            if (votesRTV.contains(event.player.uuid())) {
                votesRTV.remove(event.player.uuid());
                int cur = votesRTV.size;
                int req = (int) Math.ceil(Const.votesRatio * Groups.player.size());
                String playerName = event.player.coloredName();
                Bundle.bundled("commands.rtv.leave", playerName, cur, req);
            }

            admins.remove(event.player.uuid());
            donaters.remove(event.player.uuid());
            activeHistoryPlayers.remove(event.player.uuid());
            unitPlayer.remove(event.player.unit().id);
            ranks.remove(event.player.uuid());
            specialRanks.remove(event.player.uuid());

            Log.info("@ has disconnected from the server", event.player.name);
            String playerName = event.player.coloredName();
            Bundle.bundled("events.leave.player-leave", playerName);
        });
        // endregion

        Events.on(EventType.ServerLoadEvent.class, event -> {
            Log.info("ThedimasPlugin: Server loaded");
        });

        Events.on(EventType.WorldLoadEndEvent.class, event -> {
            history = new CacheSeq[Vars.world.width()][Vars.world.height()];
            votesRTV.clear();
        });

        Events.on(EventType.GameOverEvent.class, event -> {
            switch (Vars.state.rules.mode()) {
                case pvp -> {
                    if (!Vars.state.map.tags.getBool("hexed")) {
                        Groups.player.each(player -> player.team() == event.winner, player -> {
                            Players.incrementStats(player, "pvp");
                        });
                    }
                }
                case attack -> {
                    if (event.winner == Team.sharded) {
                        Groups.player.each(player -> {
                            Players.incrementStats(player, "attacks");
                        });
                    }
                }
                case survival -> {
                    if (Vars.state.wave >= Const.winSurvivalWaves) {
                        Groups.player.each(player -> {
                            Players.incrementStats(player, "survivals");
                        });
                    }
                }
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
            Player player = event.unit.getPlayer();
            if (player == null) {
                player = Groups.player.getByID(unitPlayer.get(event.unit.id()));
            }

            if (player == null) {
                return;
            }

            Players.incrementStats(player, "deaths");
        });

        Events.on(EventType.UnitBulletDestroyEvent.class, event -> {
            if (event.bullet.owner() instanceof Unitc unitc) {
                if (unitc.controller() instanceof Playerc player) {
                    Players.incrementStats((Player) player, "kills");
                }
            } else if (event.bullet.owner() instanceof ItemTurret.ItemTurretBuild turret) {
                if (turret.unit().controller() instanceof Playerc player) {
                    Players.incrementStats((Player) player, "kills");
                }
            }
            Log.debug(event.bullet.owner().getClass().getName());
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

                Log.info("@ put thorium into reactor (@, @)", target.name, building.tileX(), building.tileY());
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
                    Log.info("@ is building a thorium reactor near the core (@, @)", player.name, event.tile.x, event.tile.y);
                }
            }
        });
        // endregion

        Events.on(EventType.PlayerChatEvent.class, event -> {
            if (!event.message.startsWith("/")) {
                Groups.player.each(otherPlayer -> {
                    Translator.translateChatAsync(event.player, otherPlayer, event.message).thenAcceptAsync(otherPlayer::sendMessage);
                });

                Log.info(Const.chatLogFormat, Strings.stripColors(event.player.name), Strings.stripColors(event.message), event.player.locale);
                Players.incrementStats(event.player, "messages");
                DatabaseAsync.createMessageAsync(
                        Const.serverFieldName, event.player.uuid(), null, MessageType.global, event.message, event.player.locale()
                ).exceptionally(t -> {
                    Log.err(t);
                    DiscordLogger.err(t);
                    return null;
                });
            }
        });

        Events.run(EventType.Trigger.update, () -> {
            Groups.player.each(player -> {
                if (player.unit().id() != -1) {
                    unitPlayer.put(player.unit().id(), player.id());
                }
            });

            Groups.player.each(p -> {
                if (p.unit().moving()) {
                    Rank rank = ranks.get(p.uuid(), Rank.player);
                    Effect effect = rank.effect;
                    Color effectColor = rank.effectColor == null ? Color.white : rank.effectColor;
                    Call.effect(effect, p.x, p.y, 0, effectColor);
                }
            });
        });
    }
}
