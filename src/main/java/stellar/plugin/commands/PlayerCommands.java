package stellar.plugin.commands;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Structs;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import org.jooq.Field;
import stellar.database.Database;
import stellar.database.DatabaseAsync;
import stellar.database.enums.MessageType;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.StatsRecord;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.components.Rank;
import stellar.plugin.history.entry.HistoryEntry;
import stellar.plugin.history.struct.CacheSeq;
import stellar.plugin.util.menus.MenuHandler;
import stellar.plugin.util.Bundle;
import stellar.plugin.util.Players;
import stellar.plugin.util.StringUtils;
import stellar.plugin.util.Translator;

import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.world;
import static stellar.plugin.Variables.*;
import static stellar.plugin.util.StringUtils.longToTime;
import static stellar.plugin.util.StringUtils.targetColor;


public class PlayerCommands {

    public static void load(CommandHandler commandHandler) {
        commandHandler.removeCommand("t");
        commandHandler.<Player>register("t", "<text...>", "commands.t.description", (args, player) -> {
            String message = args[0];
            Groups.player.each(o -> o.team() == player.team(), otherPlayer -> {
                Translator.translateChatAsync(player, otherPlayer, message).thenAcceptAsync(msg ->
                        otherPlayer.sendMessage("<[#" + player.team().color + "]T[]>" + msg)
                );
            });

            Log.info("<T>" + Const.chatLogFormat, Strings.stripColors(player.name), Strings.stripColors(message), player.locale);
            DatabaseAsync.createMessageAsync(Const.serverFieldName, player.uuid(), player.team().name, MessageType.team, message, player.locale());
        });

        commandHandler.removeCommand("help");
        commandHandler.<Player>register("help", "[page]", "commands.help.description", (args, player) -> {
            if (args.length > 0 && !Strings.canParseInt(args[0])) {
                Bundle.bundled(player, "commands.page-not-int");
                return;
            }

            Locale locale = Bundle.findLocale(player.locale);
            int hiddenCommandsCount = player.admin ? 0 : commandHandler.getCommandList().count(c -> c.description.startsWith("commands.admin"));
            int pages = Mathf.ceil(commandHandler.getCommandList().size / Const.listPageSize - hiddenCommandsCount / Const.listPageSize);
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;

            if (--page >= pages || page < 0) {
                Bundle.bundled(player, "commands.under-page", pages);
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Bundle.format("commands.help.page", locale, page + 1, pages)).append("\n\n");

            commandHandler.getCommandList().sort(c -> c.description.startsWith("commands.admin") ? 1 : -1);

            for (int i = 6 * page; i < Math.min(6 * (page + 1), commandHandler.getCommandList().size - hiddenCommandsCount); i++) {
                CommandHandler.Command command = commandHandler.getCommandList().get(i);
                result.append("[orange] /").append(command.text).append("[white] ")
                        .append(command.paramText)
                        .append("[lightgray] - ")
                        .append(Bundle.has(command.description, locale) ? Bundle.get(command.description, locale) : command.description)
                        .append("\n");
            }
            player.sendMessage(result.toString());
        });

        commandHandler.<Player>register("tr", "[off|auto|double|somelocale]", "commands.tr.description", (args, player) -> {
            if (args.length == 0) {
                DatabaseAsync.getPlayerAsync(
                        player.uuid()
                ).thenAcceptAsync(record ->
                        Bundle.bundled(player, "commands.tr.current", record.getTranslator())
                );
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
                    );
        });

        commandHandler.<Player>register("rtv", "[on|off]", "commands.rtv.description", (args, player) -> {
            boolean rtvEnabled = Core.settings.getBool("rtv");

            if (args.length > 0) {
                if (!Variables.admins.containsKey(player.uuid())) {
                    Bundle.bundled(player, "commands.rtv.access-denied");
                    return;
                }
                rtvEnabled = !args[0].equalsIgnoreCase("off");
                Core.settings.put("rtv", rtvEnabled);
                if (!rtvEnabled && Variables.votesRTV.size > 0) {
                    Variables.votesRTV.clear();
                    Bundle.bundled("commands.rtv.votes-clear");
                }
            }

            if (!rtvEnabled) {
                Bundle.bundled(player, "commands.rtv.disabled");
                return;
            }

            if (Variables.votesRTV.contains(player.uuid())) {
                Bundle.bundled(player, "commands.rtv.already-voted");
                return;
            }

            Variables.votesRTV.add(player.uuid());
            int cur = Variables.votesRTV.size;
            int req = (int) Math.ceil(Const.votesRatio * Groups.player.size());

            String playerName = player.coloredName();
            Bundle.bundled("commands.rtv.vote", playerName, cur, req);

            if (cur >= req) {
                Variables.votesRTV.clear();
                Bundle.bundled("commands.rtv.passed");
                Events.fire(new EventType.GameOverEvent(Team.derelict));
            }
        });

        commandHandler.<Player>register("version", "commands.version.description", (arg, player) -> Bundle.bundled(player, "commands.version.msg", Const.pluginVersion));

        commandHandler.<Player>register("discord", "commands.discord.description", (args, player) -> Call.openURI(player.con, config.discordUrl));

        commandHandler.<Player>register("hub", "commands.hub.description", (args, player) -> {
            String[] address = Const.serverAddress.get("hub").split(":");
            String ip = address[0];
            int port = Strings.parseInt(address[1]);

            Call.connect(player.con, ip, port);
        });

        commandHandler.<Player>register("connect", "[list|server...]", "commands.connect.description", (args, player) -> {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                Bundle.bundled(player, "commands.connect.list", Const.serverList);
                return;
            }

            String serverName = args[0].toLowerCase();
            if (!Const.serverAddress.containsKey(serverName)) {
                Bundle.bundled(player, "commands.server-notfound", Const.serverList);
                return;
            }

            String[] address = Const.serverAddress.get(serverName).split(":");
            String ip = address[0];
            int port = Strings.parseInt(address[1]);
            Vars.net.pingHost(ip, port, host -> Call.connect(player.con, ip, port), e -> Bundle.bundled(player, "commands.connect.server-offline"));
        });

        commandHandler.<Player>register("history", "[page] [detailed]", "commands.history.description", (args, player) -> {
            boolean detailed = args.length == 2 && Structs.contains(Const.boolValues.split(", "), args[1].toLowerCase());

            if (args.length > 0 && Variables.activeHistoryPlayers.containsKey(player.uuid())) {
                if (!Strings.canParseInt(args[0])) {
                    Bundle.bundled(player, "commands.page-not-int");
                    return;
                }

                int mouseX = Mathf.clamp(Mathf.round(player.mouseX / 8), 1, world.width());
                int mouseY = Mathf.clamp(Mathf.round(player.mouseY / 8), 1, world.height());
                Locale locale = Bundle.findLocale(player.locale);

                CacheSeq<HistoryEntry> entries = Variables.getHistorySeq(mouseX, mouseY);

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
            } else if (Variables.activeHistoryPlayers.containsKey(player.uuid())) {
                Variables.activeHistoryPlayers.remove(player.uuid());
                Bundle.bundled(player, "commands.history.detailed.disabled");
            } else if (args.length == 2) {
                Variables.activeHistoryPlayers.put(player.uuid(), detailed);
                String msg = detailed ? "commands.history.detailed" : "commands.history.default";
                Bundle.bundled(player, "commands.history.enabled", msg);
            } else {
                Variables.activeHistoryPlayers.put(player.uuid(), false);
                Bundle.bundled(player, "commands.history.disabled");
            }
        });

        commandHandler.<Player>register("playtime", "[server...]", "commands.playtime.description", (args, player) -> {
            String serverColumnName;
            if (args.length > 0) {
                serverColumnName = args[0].toLowerCase();
            } else {
                serverColumnName = Const.serverFieldName;
            }

            Field<Long> field = (Field<Long>) Tables.playtime.field(serverColumnName);
            if (field == null) {
                Bundle.bundled(player, "commands.server-notfound", Const.serverList);
                return;
            }

            DatabaseAsync.getPlaytimeAsync(
                    player.uuid(), Const.playtimeField
            ).thenAcceptAsync(time -> {
                Locale locale = Bundle.findLocale(player.locale());
                Bundle.bundled(player, "commands.playtime.msg", Const.serverNames.get(serverColumnName), StringUtils.longToTime((time == null) ? 0 : time, locale));
            });
        });

        commandHandler.<Player>register("rank", "commands.rank.description", (args, player) -> {
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
                    });
                }
            }));
        });

        commandHandler.<Player>register("ranks", "commands.ranks.description", (args, player) -> {
            StringBuilder builder = new StringBuilder();
            Locale locale = Bundle.findLocale(player.locale());
            String[][] buttons = new String[Rank.values().length + 1][]; // I wanted to use Seq<String> that didn't work
            for (Rank rank : Rank.values()) {
                buttons[rank.ordinal()] = new String[]{rank.formatted(player)};
            }
            buttons[Rank.values().length] = new String[]{Bundle.get("menus.close", locale)};

            MenuHandler.send(player, Bundle.get("menus.ranks-info.title", locale), "", buttons, (menuId, option, p) -> {
                if (option >= Rank.values().length || option < 0) {
                    return;
                }

                DatabaseAsync.getStatsAsync(
                        p.uuid()
                ).thenCombineAsync(DatabaseAsync.getTotalPlaytimeAsync(p.uuid()), (record, playtime) -> {
                    String[][] newButtons = new String[][]{
                            {Bundle.get("menus.close", locale)}
                    };

                    Rank rank = Rank.values()[option];
                    int wins = record.getAttacks() + record.getSurvivals() + record.getHexWins() + record.getPvp();
                    String message = Bundle.format("commands.ranks.rank-info", locale,
                            rank.formatted(p),
                            targetColor(wins, rank.requirements.wins), wins, rank.requirements.wins,
                            targetColor(record.getWaves(), rank.requirements.waves), record.getWaves(), rank.requirements.waves,
                            targetColor(record.getBuilt(), rank.requirements.built), record.getBuilt(), rank.requirements.built,
                            targetColor(playtime.intValue(), rank.requirements.playtime * 60), longToTime(playtime, locale), longToTime(rank.requirements.playtime * 60L, locale)
                    );
                    Call.menu(p.con(), 0, Bundle.get("menus.rank-info.title", locale), message, newButtons);
                    return null;
                });
            });
        });

        commandHandler.<Player>register("stats", "commands.stats.description", (args, player) -> {
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
                                statsRecord.getLogins(), statsRecord.getMessages(), statsRecord.getDeaths(), StringUtils.longToTime(playtime, locale));

                        if (specialRank != null) {
                            message = Bundle.format("commands.stats.msg.with-status",
                                    Bundle.findLocale(player.locale()), record.getId(), player.coloredName(),
                                    rank.formatted(player), specialRank.formatted(player),
                                    statsRecord.getBuilt(), statsRecord.getBroken(),
                                    statsRecord.getAttacks(), statsRecord.getSurvivals(), statsRecord.getHexWins(), statsRecord.getWaves(),
                                    statsRecord.getLogins(), statsRecord.getMessages(), statsRecord.getDeaths(), StringUtils.longToTime(playtime, locale));
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
                    }));
        });

        commandHandler.<Player>register("msg", "<player_name> <message...>", "commands.msg.description", (args, player) -> {
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
                    Translator.translateRawAsync(player, target, args[1]).thenAcceptAsync(msg -> {
                        Bundle.bundled(target, "commands.msg.from", Strings.stripColors(player.name()), String.format(targetDetailed ? "%s (%s)" : "%s", msg, args[1]));
                    });
                    return null;
                })
            );
        });

        // region debug commands
        // TODO: effect, set block/floor/overlay commands
        if (Core.settings.getBool("debug")) {
            commandHandler.<Player>register("test-menu", "[some-text...]", "Test menu", (args, player) -> {
                String[][] buttons = new String[][]{
                        {"A", "B", "C"},
                        {"D", "E"}
                };

                MenuHandler.send(player, "Test", args.length > 0 ? args[0] : "None", buttons, (menuId, option, p) -> {
                    p.sendMessage(String.format("%d: %d", menuId, option));
                });
            });

            commandHandler.<Player>register("setrank", "<rank>", "Set your rank temporary. [accent]Debug only![]", (args, player) -> {
                try {
                    Variables.ranks.put(player.uuid(), Rank.valueOf(args[0]));
                    player.sendMessage(String.format("Your new rank is %s", args[0]));
                } catch (IllegalArgumentException e) {
                    player.sendMessage("not found");
                }
            });

            commandHandler.<Player>register("corestats", "[id]", "Get core stats for you or specified player by ID.", (args, player) -> {
                Player target = player;
                if (args.length >= 1) {
                    if (!Strings.canParseInt(args[0])) {
                        Bundle.bundled(player, "commands.incorrect-format.number");
                        return;
                    }

                    UsersRecord record;
                    try {
                        record = Database.getPlayer(Strings.parseInt(args[0]));
                    } catch (SQLException e) {
                        Log.err(e);
                        player.sendMessage("[scarlet]AAAAAA...[]");
                        return;
                    }

                    target = record != null ? Players.getPlayer(record.getUuid()) : null;
                    if (target == null) {
                        Bundle.bundled(player, "commands.player-notfound");
                        return;
                    }
                }

                StringBuilder builder = new StringBuilder();
                ObjectMap<String, Integer> stats = Variables.statsData.get(target.uuid());

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