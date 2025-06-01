package stellar.plugin.commands;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.struct.BoolSeq;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration;
import mindustry.net.Packets;
import org.jooq.Field;
import stellar.database.Database;
import stellar.database.DatabaseAsync;
import stellar.database.enums.MessageType;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.menus.func.MenuRunner;
import stellar.menus.types.Menu;
import stellar.plugin.Const;
import stellar.plugin.components.Rank;
import stellar.plugin.components.history.entry.HistoryEntry;
import stellar.plugin.components.history.struct.CacheSeq;
import stellar.plugin.type.ServerInfo;
import stellar.plugin.type.VoteSession;
import stellar.plugin.util.Players;
import stellar.plugin.util.StringUtils;
import stellar.plugin.util.Translator;
import stellar.plugin.util.commands.Command;
import stellar.plugin.util.logger.DiscordLogger;
import stellar.plugin.util.menus.MenuHandler;
import thedimas.util.Bundle;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.world;
import static mindustry.core.NetServer.voteCooldown;
import static stellar.plugin.Variables.*;
import static stellar.plugin.util.StringUtils.longToTime;
import static stellar.plugin.util.StringUtils.targetColor;


public class PlayerCommands {
    public static void load(CommandHandler commandHandler) {
        commandHandler.removeCommand("t");
        commandManager.registerPlayer("t", "<text...>", "commands.t.description", (args, player) -> {
            String message = args[0];
            Groups.player.each(o -> o.team() == player.team(), otherPlayer -> {
                Translator.translateChatAsync(player, otherPlayer, message).thenAcceptAsync(msg ->
                        otherPlayer.sendMessage("<[#" + player.team().color + "]T[]>" + msg)
                );
            });

            Log.info("<T>" + Const.chatLogFormat, Strings.stripColors(player.name), Strings.stripColors(message), player.locale);
            DatabaseAsync.createMessageAsync(
                    Const.serverFieldName, player.uuid(), player.team().name, MessageType.team, message, player.locale()
            ).exceptionally(t -> {
                Log.err(t);
                DiscordLogger.err(t);
                return null;
            });
        });

        commandHandler.removeCommand("help");
        commandManager.registerPlayer("help", "[page]", "commands.help.description", (args, player) -> {
            if (args.length > 0 && !Strings.canParseInt(args[0])) {
                Bundle.bundled(player, "commands.page-not-int");
                return;
            }

            Seq<Command> legacyCommands = commandHandler.getCommandList()
                    .select(c -> !commandManager.commandExists(c.text))
                    .map(Command::fromArc);

            Seq<Command> commands = legacyCommands
                    .add(commandManager.getCommandList()
                            .sort(c -> commandHandler.getCommandList().map(c1 -> c1.text).indexOf(c.getName())) // keeping same order
                            .sort(c -> c.getRank().ordinal())
                    );
            Locale locale = Bundle.findLocale(player.locale);
            int hiddenCommandsCount = player.admin ? 0 : commands.count(c -> !c.isAllowed(player));
            int pages = Mathf.ceil(commands.size / Const.listPageSize - hiddenCommandsCount / Const.listPageSize);
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;

            if (--page >= pages || page < 0) {
                Bundle.bundled(player, "commands.under-page", pages);
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Bundle.format("commands.help.page", locale, page + 1, pages)).append("\n\n");

            for (int i = 6 * page; i < Math.min(6 * (page + 1), commands.size - hiddenCommandsCount); i++) {
                Command command = commands.get(i);
                result.append("[orange] /").append(command.getName()).append("[white] ")
                        .append(command.getParamText())
                        .append("[lightgray] - ")
                        .append(Bundle.has(command.getDescription(), locale) ? Bundle.get(command.getDescription(), locale) : command.getDescription())
                        .append("[white] ~ ") // TODO: maybe remove this, idk
                        .append(command.getRank().formatted(player))
                        .append("\n");
            }
            player.sendMessage(result.toString());
        });

        commandManager.registerPlayer("rules", "commands.rules.description", (args, player) -> {
            Locale locale = Bundle.findLocale(player.locale);
            String rules = Bundle.format("rules", locale);

            Call.infoMessage(player.con, Bundle.format("commands.rules.list", locale, rules));
        });

        commandManager.registerPlayer("tr", "[off|auto|double|somelocale]", "commands.tr.description", (args, player) -> {
            if (args.length == 0) {
                DatabaseAsync.getPlayerAsync(
                        player.uuid()
                ).thenAcceptAsync(record ->
                        Bundle.bundled(player, "commands.tr.current", record.getTranslator())
                ).exceptionally(t -> {
                    Log.err(t);
                    DiscordLogger.err(t);
                    return null;
                });
                return;
            }

            String mode = args[0].toLowerCase();
            if (Seq.with("off", "auto", "double").contains(mode)) {
                Bundle.bundled(player, "commands.tr." + mode);
            } else {
                String target = Const.translatorLocales.get(mode);
                if (target == null) {
                    Bundle.bundled(player, "commands.tr.list", String.join(", ", Const.translatorLocales.keys())); // TODO: settings menu
                    return;
                }
                Bundle.bundled(player, "commands.tr.set", target);
            }

            DatabaseAsync.getContextAsync().thenAcceptAsync(context -> context
                    .update(Tables.users)
                    .set(Tables.users.translator, mode)
                    .where(Tables.users.uuid.eq(player.uuid()))
                    .executeAsync()
            ).exceptionally(t -> {
                Log.err(t);
                DiscordLogger.err(t);
                return null;
            });
        });

        commandManager.registerPlayer("rtv", "[on|off]", "commands.rtv.description", (args, player) -> {
            boolean rtvEnabled = Core.settings.getBool("rtv");

            if (args.length > 0) {
                if (!admins.containsKey(player.uuid())) {
                    Bundle.bundled(player, "commands.rtv.access-denied");
                    return;
                }
                rtvEnabled = !args[0].equalsIgnoreCase("off");
                Core.settings.put("rtv", rtvEnabled);
                if (!rtvEnabled && votesRTV.size > 0) {
                    votesRTV.clear();
                    Bundle.bundled("commands.rtv.votes-clear");
                }
            }

            BoolSeq RTVmaps = new BoolSeq(skippedMaps);
            RTVmaps.reverse();
            boolean unableToSkip = RTVmaps.size > 1 ? (RTVmaps.get(0) && RTVmaps.get(1)) || (RTVmaps.get(1) && RTVmaps.get(2)) : false;

            if (!rtvEnabled || unableToSkip) {
                Bundle.bundled(player, "commands.rtv.disabled");
                return;
            }

            if (votesRTV.contains(player.uuid())) {
                Bundle.bundled(player, "commands.rtv.already-voted");
                return;
            }

            votesRTV.add(player.uuid());
            int cur = votesRTV.size;
            int req = (int) Math.ceil(Const.votesRatio * Groups.player.size());

            String playerName = player.coloredName();
            Bundle.bundled("commands.rtv.vote", playerName, cur, req);

            if (cur >= req) {
                votesRTV.clear();
                Bundle.bundled("commands.rtv.passed");
                Events.fire(new EventType.GameOverEvent(Team.derelict));
            }
        });

        commandManager.registerPlayer("version", "commands.version.description", (arg, player) -> Bundle.bundled(player, "commands.version.msg", Const.pluginVersion));

        commandManager.registerPlayer("discord", "commands.discord.description", (args, player) -> Call.openURI(player.con, config.discordUrl));

        commandManager.registerPlayer("hub", "commands.hub.description", (args, player) -> {
            ServerInfo serverInfo = Const.servers.find(i -> i.getId().equalsIgnoreCase("hub"));
            Call.connect(player.con, serverInfo.getDomain(), serverInfo.getPort());
        });

        commandManager.registerPlayer("connect", "[list|server...]", "commands.connect.description", (args, player) -> {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                Bundle.bundled(player, "commands.connect.list", "[white]" + String.join("\n", Const.servers.map(ServerInfo::getName)));
                return;
            }

            ServerInfo serverInfo = Const.servers.find(i -> i.getName().equalsIgnoreCase(args[0]));
            if (serverInfo == null) {
                Bundle.bundled(player, "commands.server-notfound", "[white]" + String.join("\n", Const.servers.map(ServerInfo::getName)));
                return;
            }

            Vars.net.pingHost(serverInfo.getDomain(), serverInfo.getPort(),
                    host -> Call.connect(player.con, serverInfo.getDomain(), serverInfo.getPort()),
                    e -> Bundle.bundled(player, "commands.connect.server-offline")
            );
        });

        commandManager.registerPlayer("history", "[page] [detailed]", "commands.history.description", (args, player) -> {
            boolean detailed = args.length == 2 && Structs.contains(Const.boolValues.split(", "), args[1].toLowerCase());

            if (args.length > 0 && activeHistoryPlayers.containsKey(player.uuid())) {
                if (!Strings.canParseInt(args[0])) {
                    Bundle.bundled(player, "commands.page-not-int");
                    return;
                }

                int mouseX = Mathf.clamp(Mathf.round(player.mouseX / 8), 1, world.width());
                int mouseY = Mathf.clamp(Mathf.round(player.mouseY / 8), 1, world.height());
                Locale locale = Bundle.findLocale(player.locale);

                CacheSeq<HistoryEntry> entries = getHistorySeq(mouseX, mouseY);

                int page = Strings.parseInt(args[0]) - 1;
                int pages = Mathf.ceil(entries.size / Const.listPageSize);

                if (page >= pages || pages < 0 || page < 0) {
                    Bundle.bundled(player, "commands.under-page", page);
                    return;
                }

                StringBuilder result = new StringBuilder();
                result.append(Bundle.format("commands.history.page", locale, mouseX, mouseY, page + 1, pages)).append("\n");

                if (entries.isEmpty()) {
                    result.append(Bundle.get("history.empty", locale)).append("\n");
                }

                for (int i = 6 * page; i < Math.min(Const.listPageSize * (page + 1), entries.size); i++) {
                    HistoryEntry entry = entries.get(i);
                    result.append(entry.getMessage(locale));
                    if (detailed) {
                        result.append(Bundle.format("history.timestamp", locale, entry.getTimestamp(TimeUnit.MINUTES)));
                    }
                    result.append("\n");
                }

                player.sendMessage(result.toString());
            } else if (activeHistoryPlayers.containsKey(player.uuid())) {
                activeHistoryPlayers.remove(player.uuid());
                Bundle.bundled(player, "commands.history.detailed.disabled");
            } else if (args.length == 2) {
                activeHistoryPlayers.put(player.uuid(), detailed);
                String msg = detailed ? "commands.history.detailed" : "commands.history.default";
                Bundle.bundled(player, "commands.history.enabled", msg);
            } else {
                activeHistoryPlayers.put(player.uuid(), false);
                Bundle.bundled(player, "commands.history.disabled");
            }
        });

        commandManager.registerPlayer("playtime", "[server...]", "commands.playtime.description", (args, player) -> {
            ServerInfo serverInfo = args.length > 0 ?
                    Const.servers.find(i -> i.getName().equalsIgnoreCase(args[0])) :
                    Const.servers.find(i -> i.getId().equals(Const.serverFieldName));

            Field<Long> field = serverInfo != null ? (Field<Long>) Tables.playtime.field(serverInfo.getId()) : null;
            if (field == null) {
                Bundle.bundled(player, "commands.server-notfound", "[white]" + String.join("\n", Const.servers.map(ServerInfo::getName)));
                return;
            }

            DatabaseAsync.getPlaytimeAsync(
                    player.uuid(), field 
            ).thenAcceptAsync(time -> {
                Locale locale = Bundle.findLocale(player.locale());
                Bundle.bundled(player, "commands.playtime.msg", serverInfo.getNameFormatted(), longToTime((time == null) ? 0 : time, locale));
            }).exceptionally(t -> {
                Log.err(t);
                DiscordLogger.err(t);
                return null;
            });
        });

        commandManager.registerPlayer("rank", "commands.rank.description", (args, player) -> {
            Rank rank = ranks.get(player.uuid(), Rank.player); // TODO: Async
            Locale locale = Bundle.findLocale(player.locale());
            String[][] buttons = {
                    {Bundle.get("commands.rank.next-rank", locale)},
                    {Bundle.get("menus.close", locale)}
            };

            MenuHandler.send(player, Bundle.get("menus.rank-info.title", locale), Bundle.format("commands.rank.msg", locale, rank.formatted(player)), buttons, ((menuId, option, p) -> {
                if (option == -1) {
                    return;
                }
                Rank nextRank = rank.getNext();
                String[][] newButtons = new String[][]{
                        {Bundle.get("menus.close", locale)}
                };

                if (nextRank == null) {
                    Call.menu(p.con(), 0, Bundle.get("menus.rank-info.title", locale), Bundle.format("commands.rank.next-rank.none", locale), newButtons);
                } else {
                    DatabaseAsync.getStatsAsync(
                            p.uuid()
                    ).thenCombineAsync(DatabaseAsync.getTotalPlaytimeAsync(p.uuid()), (record, playtime) -> {
                        int wins = record.getAttacks() + record.getSurvivals() + record.getHexWins() + record.getPvp();
                        String message = Bundle.format("commands.rank.next-rank.info", locale,
                                nextRank.formatted(p),
                                targetColor(wins, nextRank.requirements.wins), wins, nextRank.requirements.wins,
                                targetColor(record.getWaves(), nextRank.requirements.waves), record.getWaves(), nextRank.requirements.waves,
                                targetColor(record.getBuilt(), nextRank.requirements.built), record.getBuilt(), nextRank.requirements.built,
                                targetColor(playtime.intValue(), nextRank.requirements.playtime * 60), longToTime(playtime, locale), longToTime(nextRank.requirements.playtime * 60L, locale)
                        );
                        Call.menu(p.con(), 0, Bundle.get("menus.rank-info.title", locale), message, newButtons);
                        return null;
                    }).exceptionally(t -> {
                        Log.err(t);
                        DiscordLogger.err(t);
                        return null;
                    });
                }
            }));
        });

        commandManager.registerPlayer("ranks", "commands.ranks.description", (args, player) -> {
            Locale locale = Bundle.findLocale(player.locale());
            Seq<Rank> ranks = Seq.select(Rank.values(), r -> !r.special);
            Seq<Rank> specRanks = Seq.select(Rank.values(), r -> r.special);

            String[][] buttons = new String[ranks.size + 2][]; // I wanted to use Seq<String> that didn't work
            ranks.retainAll(r -> !r.special).each(rank -> {
                buttons[rank.ordinal()] = new String[]{rank.formatted(player)};
            });

            String title = Bundle.get("menus.ranks-info.title", locale);
            String close = Bundle.get("menus.close", locale);

            buttons[ranks.size] = new String[]{Bundle.get("menus.special-ranks", locale)};
            buttons[ranks.size + 1] = new String[]{close};

            String[][] closeButton = new String[][]{
                    {Bundle.get("menus.close", locale)}
            };

            MenuHandler.send(player, title, "", buttons, (menuId, option, p) -> {
                if (option > Rank.values().length || option < 0) {
                    return;
                }

                if (option == ranks.size) {
                    String[][] newButtons = new String[specRanks.size + 1][];
                    for (int i = 0; i < specRanks.size; i++) {
                        newButtons[i] = new String[]{specRanks.get(i).formatted(player)};
                    }
                    newButtons[specRanks.size] = new String[]{close};

                    MenuHandler.send(player, title, "", newButtons, (m, o, pl) -> {
                        if (o >= specRanks.size || o < 0) {
                            return;
                        }
                        Rank rank = specRanks.get(o);
                        String message = Bundle.format("commands.ranks.special-info", locale, rank.formatted(pl));
                        Call.menu(pl.con(), 0, Bundle.get("menus.rank-info.title", locale), message, closeButton);
                    });
                    return;
                }

                DatabaseAsync.getStatsAsync(
                        p.uuid()
                ).thenCombineAsync(DatabaseAsync.getTotalPlaytimeAsync(p.uuid()), (record, playtime) -> {
                    Rank rank = Rank.values()[option];
                    int wins = record.getAttacks() + record.getSurvivals() + record.getHexWins() + record.getPvp();
                    String message = Bundle.get("commands.ranks.special-info", locale);
                    if (rank.requirements != null) {
                        message = Bundle.format("commands.ranks.rank-info", locale,
                                rank.formatted(p),
                                targetColor(wins, rank.requirements.wins), wins, rank.requirements.wins,
                                targetColor(record.getWaves(), rank.requirements.waves), record.getWaves(), rank.requirements.waves,
                                targetColor(record.getBuilt(), rank.requirements.built), record.getBuilt(), rank.requirements.built,
                                targetColor(playtime.intValue(), rank.requirements.playtime * 60), longToTime(playtime, locale), longToTime(rank.requirements.playtime * 60L, locale)
                        );
                    }

                    Call.menu(p.con(), 0, Bundle.get("menus.rank-info.title", locale), message, closeButton);
                    return null;
                }).exceptionally(t -> {
                    Log.err(t);
                    DiscordLogger.err(t);
                    return null;
                });
            });
        });

        commandManager.registerPlayer("stats", "commands.stats.description", (args, player) -> {
            DatabaseAsync.getPlayerAsync(
                    player.uuid()
            ).thenCombineAsync(DatabaseAsync.getStatsAsync(player.uuid()), (record, statsRecord) ->
                    DatabaseAsync.getTotalPlaytimeAsync(player.uuid()).thenAcceptAsync(playtime -> {
                        Rank rank = ranks.get(player.uuid(), Rank.player);
                        Rank specialRank = specialRanks.get(player.uuid());
                        Locale locale = Bundle.findLocale(player.locale);
                        String message = Bundle.format("commands.stats.msg",
                                Bundle.findLocale(player.locale()), record.getId(), player.coloredName(),
                                rank.formatted(player),
                                statsRecord.getBuilt(), statsRecord.getBroken(),
                                statsRecord.getAttacks(), statsRecord.getSurvivals(), statsRecord.getHexWins(), statsRecord.getWaves(),
                                statsRecord.getLogins(), statsRecord.getMessages(), statsRecord.getDeaths(), longToTime(playtime, locale));

                        if (specialRank != null) {
                            message = Bundle.format("commands.stats.msg.with-status",
                                    Bundle.findLocale(player.locale()), record.getId(), player.coloredName(),
                                    rank.formatted(player), specialRank.formatted(player),
                                    statsRecord.getBuilt(), statsRecord.getBroken(),
                                    statsRecord.getAttacks(), statsRecord.getSurvivals(), statsRecord.getHexWins(), statsRecord.getWaves(),
                                    statsRecord.getLogins(), statsRecord.getMessages(), statsRecord.getDeaths(), longToTime(playtime, locale));
                        }

                        String[][] buttons = {
                                {Bundle.get("commands.stats.hex", locale)},
                                {Bundle.get("commands.rank.next-rank", locale)},
                                {Bundle.get("menus.close", locale)}
                        };

                        MenuHandler.send(player, Bundle.get("menus.stats.title", locale), message, buttons, (menuId, option, p) -> {
                            if (option == -1 || option == 2) {
                                return;
                            }

                            String[][] newButtons = new String[][]{
                                    {Bundle.get("menus.close", locale)}
                            };

                            if (option == 0) {
                                String msg = Bundle.format("commands.stats.msg.hex", locale,
                                        statsRecord.getHexesCaptured(), statsRecord.getHexesLost(), statsRecord.getHexesDestroyed(),
                                        statsRecord.getHexWins(), statsRecord.getHexLosses()); // TODO: hex score
                                Call.menu(p.con(), 0, Bundle.get("menus.stats.title", locale), msg, newButtons);
                            } else if (option == 1) {
                                Rank nextRank = rank.getNext();
                                if (nextRank == null) {
                                    Call.menu(p.con(), 0, Bundle.get("menus.rank-info.title", locale), Bundle.format("commands.rank.next-rank.none", locale), newButtons);
                                } else {
                                    int wins = statsRecord.getAttacks() + statsRecord.getSurvivals() + statsRecord.getHexWins() + statsRecord.getPvp();
                                    String msg = Bundle.format("commands.rank.next-rank.info", locale,
                                            nextRank.formatted(p),
                                            targetColor(wins, nextRank.requirements.wins), wins, nextRank.requirements.wins,
                                            targetColor(statsRecord.getWaves(), nextRank.requirements.waves), statsRecord.getWaves(), nextRank.requirements.waves,
                                            targetColor(statsRecord.getBuilt(), nextRank.requirements.built), statsRecord.getBuilt(), nextRank.requirements.built,
                                            targetColor(playtime.intValue(), nextRank.requirements.playtime * 60), longToTime(playtime, locale), longToTime(nextRank.requirements.playtime * 60L, locale)
                                    );
                                    Call.menu(p.con(), 0, Bundle.get("menus.rank-info.title", locale), msg, newButtons);
                                }
                            }
                        });
                    })
            ).exceptionally(t -> {
                Log.err(t);
                DiscordLogger.err(t);
                return null;
            });
        });

        commandManager.registerPlayer("msg", "<player_name> <message...>", "commands.msg.description", (args, player) -> {
            Player target = Players.findPlayer(StringUtils.stripColorsAndGlyphs(args[0]).strip());
            if (target == null) {
                Bundle.bundled(player, "commands.player-notfound");
                return;
            }

            if (player == target) {
                Bundle.bundled(player, "commands.msg.self");
                return;
            }

            Translator.translateRawAsync(
                    player, target, args[1]
            ).thenAcceptAsync(translated ->
                    DatabaseAsync.getPlayerAsync(
                            player.uuid()
                    ).thenCombineAsync(DatabaseAsync.getPlayerAsync(target.uuid()), (playerInfo, targetInfo) -> {
                        boolean playerDetailed = playerInfo.getTranslator().equals("double");
                        boolean targetDetailed = targetInfo.getTranslator().equals("double");
                        Bundle.bundled(player, "commands.msg.to", Strings.stripColors(target.name()), String.format(playerDetailed ? "%s (%s)" : "%s", args[1], args[1]));
                        Bundle.bundled(target, "commands.msg.from", Strings.stripColors(player.name()), String.format(targetDetailed ? "%s (%s)" : "%s", translated, args[1]));
                        return null;
                    })
            ).exceptionally(t -> {
                Log.err(t);
                DiscordLogger.err(t);
                return null;
            });
        });

        commandManager.registerPlayer("players", "commands.players.description", (args, player) -> {
            for (int i = 0; i < Groups.player.size(); ) {
                Player other = Groups.player.index(i);
                player.sendMessage(String.format("[yellow]%d.[] %s[white] - [accent]%d[][]", ++i, other.coloredName(), other.id()));
            }
        });

        commandHandler.removeCommand("votekick");
        commandManager.registerPlayer("votekick", "<player> <reason...>", "commands.votekick.description", (args, player) -> {
            if (!Administration.Config.enableVotekick.bool()) {
                Bundle.bundled(player, "commands.votekick.disabled");
                return;
            }

            if (voteSession != null) {
                Bundle.bundled(player, "commands.votekick.already-voting");
                return;
            }

            if (Groups.player.size() < 3) {
                Bundle.bundled(player, "commands.votekick.min-players");
                return;
            }

            Player target;
            if (args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))) {
                int id = Strings.parseInt(args[0].substring(1));
                target = Groups.player.find(p -> p.id() == id);
            } else {
                target = Players.findPlayer(args[0]);
            }

            if (target == null) {
                Bundle.bundled(player, "commands.player-notfound");
                return;
            }

            if (target.id() == player.id()) {
                Bundle.bundled(player, "commands.votekick.self");
                return;
            }

            if (target.team() != player.team()) {
                Bundle.bundled(player, "commands.votekick.not-same-team");
                return;
            }

            // Probably use special rank type for admin verification
            if (player.admin() || specialRanks.containsKey(player.uuid())) { // instead of writing the logic again, just call the event
                Events.fire(new EventType.AdminRequestEvent(player, target, Packets.AdminAction.ban));
                return;
            }

            if (target.admin() || specialRanks.containsKey(target.uuid())) {
                Bundle.bundled(player, "commands.player-admin");
                return;
            }

            Timekeeper vtime = voteCooldowns.get(player.uuid(), () -> new Timekeeper(voteCooldown));

            if (!vtime.get()) {
                Bundle.bundled(player, "commands.votekick.cooldown", longToTime(voteCooldown, Bundle.findLocale(player.locale()))); // TODO: i18n
                return;
            }

            VoteSession session = new VoteSession(player, target, args[1]);
            session.vote(player, 1);
            Bundle.bundled(player, "commands.votekick.reason", args[1]); // TODO: i18n
            vtime.reset();
            voteSession = session;
        });

        commandHandler.removeCommand("vote");
        commandManager.registerPlayer("vote", "<y/n/c>", "commands.vote.description", (args, player) -> {
            if (voteSession == null) {
                Bundle.bundled(player, "commands.votekick.no-voting");
                return;
            }

            if ((player.admin || specialRanks.containsKey(player.uuid())) && args[0].equalsIgnoreCase("c")) {
                Bundle.bundled(player, "commands.votekick.canceled", player.coloredName());
                voteSession.task.cancel();
                voteSession = null;
                return;
            }

            if (voteSession.voted.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.votekick.already-voted");
                return;
            }

            if (voteSession.target == player) {
                Bundle.bundled(player, "commands.votekick.self");
                return;
            }

            if (voteSession.target.team() != player.team()) {
                Bundle.bundled(player, "commands.votekick.not-same-team", player.team().name);
                return;
            }

            switch (args[0]) {
                case "y" -> {
                    voteSession.vote(player, 1);
                }
                case "n" -> {
                    voteSession.vote(player, -1);
                }
                default -> {
                    Bundle.bundled(player, "commands.vote.invalid-arg");
                }
            }
        });

        // region debug commands
        // TODO: effect, set block/floor/overlay commands
        if (Core.settings.getBool("debug")) {
            commandManager.registerPlayer("menu2", "Another test menu", (args, player) -> {
                String[][] buttons = new String[][]{
                        {"A", "B", "C"},
                        {"<", ">", "x"}
                };
                var anotherMenu = new Menu(player, "2nd menu", "Some abracadabra", buttons, false, -1);
                var coreMenu = stellar.menus.MenuHandler.menu(player, "Test", "Some abracadabra", buttons, true, MenuRunner.none);

                MenuRunner runner3 = (menuId, option, p) -> anotherMenu.show();
                MenuRunner runner4 = (menuId, option, p) -> coreMenu.show();
                MenuRunner close = (menuId, option, p) -> stellar.menus.MenuHandler.closeMenu(p, menuId);

                anotherMenu.onInteract((menuId, option, p) -> p.sendMessage(String.format("[accent]%d[]: %d", menuId, option)))
                            .onButton(3, runner3)
                            .onButton(4, runner4)
                            .onButton(5, close);
                coreMenu.onInteract((menuId, option, p) -> p.sendMessage(String.format("[accent]%d[]: %d", menuId, option)))
                        .onButton(3, runner3)
                        .onButton(4, runner4)
                        .onButton(5, close);
            });

            commandManager.registerPlayer("test-menu", "[some-text...]", "Test menu", (args, player) -> {
                String[][] buttons = new String[][]{
                        {"A", "B", "C"},
                        {"D", "E"}
                };

                MenuHandler.send(player, "Test", args.length > 0 ? args[0] : "None", buttons, (menuId, option, p) -> {
                    p.sendMessage(String.format("%d: %d", menuId, option));
                });
            });

            commandManager.registerPlayer("setrank", "<rank>", "Set your rank temporary. [accent]Debug only![]", (args, player) -> {
                try {
                    ranks.put(player.uuid(), Rank.valueOf(args[0]));
                    player.sendMessage(String.format("Your new rank is %s", args[0]));
                } catch (IllegalArgumentException e) {
                    player.sendMessage("not found");
                }
            });

            commandManager.registerPlayer("corestats", "[id]", "Get core stats for you or specified player by ID.", (args, player) -> {
                Player target = player;
                if (args.length >= 1) {
                    if (!Strings.canParseInt(args[0])) {
                        Bundle.bundled(player, "commands.incorrect-format.number");
                        return;
                    }

                    UsersRecord record = Database.getPlayer(Strings.parseInt(args[0]));
                    target = record != null ? Players.getPlayer(record.getUuid()) : null;
                    if (target == null) {
                        Bundle.bundled(player, "commands.player-notfound");
                        return;
                    }
                }

                StringBuilder builder = new StringBuilder();
                ObjectMap<String, Integer> stats = statsData.get(target.uuid());

                if (stats == null) {
                    player.sendMessage("[teal]Null...[]");
                    return;
                } else if (stats.isEmpty()) {
                    player.sendMessage("[teal]Empty...[]");
                    return;
                }

                builder.append(target.coloredName()).append("[white]:\n");
                stats.each((stat, value) -> {
                    builder.append(String.format(" - %s: [%s]%s[]", stat, value == 0 ? "white" : "accent", value)).append("\n");
                });
                player.sendMessage(builder.toString());
            });
        }
        // endregion
    }
}
