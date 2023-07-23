package stellar.plugin.command;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
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
import stellar.database.enums.MessageType;
import stellar.database.gen.tables.records.StatsRecord;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.database.Database;
import stellar.database.gen.Tables;
import stellar.plugin.components.Rank;
import stellar.plugin.history.entry.HistoryEntry;
import stellar.plugin.history.struct.CacheSeq;
import stellar.plugin.menus.MenuHandler;
import stellar.plugin.util.Bundle;
import stellar.plugin.util.Players;
import stellar.plugin.util.StringUtils;
import stellar.plugin.util.Translator;
import stellar.plugin.util.logger.DiscordLogger;

import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.*;
import static stellar.plugin.Variables.config;
import static stellar.plugin.Variables.jsallowed;
import static stellar.plugin.util.NetUtils.updateBackground;
import static stellar.plugin.util.StringUtils.longToTime;
import static stellar.plugin.util.StringUtils.targetColor;


public class PlayerCommands {

    public static void load(CommandHandler commandHandler) {
        commandHandler.removeCommand("t");
        commandHandler.<Player>register("t", "<text...>", "commands.t.description", (args, player) -> {
            String message = args[0];
            Groups.player.each(o -> o.team() == player.team(), otherPlayer -> {
                new Thread(() -> {
                    String msg = Translator.translateChat(player, otherPlayer, message);
                    otherPlayer.sendMessage("<[#" + player.team().color + "]T[]>" + msg);
                }).start();
            });

            Log.info("<T>" + Const.chatLogFormat, Strings.stripColors(player.name), Strings.stripColors(message), player.locale);
            new Thread(() -> {
                try {
                    Database.createMessage(Const.serverFieldName, player.uuid(), player.team().name, MessageType.team, message, player.locale());
                } catch (SQLException e) {
                    Log.err(e);
                }
            }).start();
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
            String locale;
            try {
                locale = Database.getPlayer(player.uuid()).getTranslator();
            } catch (Throwable t) {
                Bundle.bundled(player, "commands.tr.error");
                Log.err(t);
                DiscordLogger.err(t);
                return;
            }

            if (args.length == 0) {
                Bundle.bundled(player, "commands.tr.current", locale);
                return;
            }

            String mode = args[0].toLowerCase();
            switch (mode) {
                case "off" -> {
                    try {
                        updateBackground(Database.getContext()
                                .update(Tables.users)
                                .set(Tables.users.translator, "off")
                                .where(Tables.users.uuid.eq(player.uuid())));
                    } catch (Throwable t) {
                        Log.err(t);
                        DiscordLogger.err(t);
                    }
                    Bundle.bundled(player, "commands.tr.disabled");
                }
                case "auto" -> {
                    try {
                        updateBackground(Database.getContext()
                                .update(Tables.users)
                                .set(Tables.users.translator, "auto")
                                .where(Tables.users.uuid.eq(player.uuid())));
                    } catch (Throwable t) {
                        Log.err(t);
                        DiscordLogger.err(t);
                    }
                    Bundle.bundled(player, "commands.tr.auto");
                }
                case "double" -> {
                    try {
                        updateBackground(Database.getContext()
                                .update(Tables.users)
                                .set(Tables.users.translator, "double")
                                .where(Tables.users.uuid.eq(player.uuid())));
                    } catch (Throwable t) {
                        Log.err(t);
                        DiscordLogger.err(t);
                    }
                    Bundle.bundled(player, "commands.tr.double");
                }
                default -> {
                    Locale target = Structs.find(locales, l -> mode.equalsIgnoreCase(l.toString()));
                    if (target == null) {
                        Bundle.bundled(player, "commands.tr.list", Const.LocaleListHolder.localeList);
                        return;
                    }
                    try {
                        updateBackground(Database.getContext()
                                .update(Tables.users)
                                .set(Tables.users.translator, target.toString())
                                .where(Tables.users.uuid.eq(player.uuid())));
                    } catch (Throwable t) {
                        Log.err(t);
                        DiscordLogger.err(t);
                    }
                    Bundle.bundled(player, "commands.tr.set", target);
                }
            }
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

            try {
                Long time = Database.getPlaytime(player.uuid(), Const.playtimeField);
                Locale locale = Bundle.findLocale(player.locale());
                Bundle.bundled(player, "commands.playtime.msg", Const.serverNames.get(serverColumnName), StringUtils.longToTime((time == null) ? 0 : time, locale));
            } catch (Throwable t) {
                Log.err("Failed to get playtime for player '" + player.uuid() + "'", t);
            }
        });

        commandHandler.<Player>register("rank", "commands.rank.description", (args, player) -> {
            try {
                Rank rank = Rank.getRank(player);
                Locale locale = Bundle.findLocale(player.locale());
                String[][] buttons = {
                        {Bundle.get("commands.rank.next-rank", locale)},
                        {Bundle.get("menus.close", locale)}
                };

                MenuHandler.send(player, Bundle.get("menus.rank-info.title", locale), Bundle.format("commands.rank.msg", locale, rank.formatted(player)), buttons, ((menuId, option, p) -> {
                    if (option == -1) {
                        return;
                    }

                    try {
                        Rank nextRank = Rank.getRank(p).getNext();
                        String[][] newButtons = new String[][] {
                                {Bundle.get("menus.close", locale)}
                        };

                        if (nextRank == null) {
                            Call.menu(p.con(), 0, Bundle.get("menus.rank-info.title", locale), Bundle.format("commands.rank.next-rank.none", locale), newButtons);
                        } else {
                            StatsRecord record = Database.getStats(p.uuid());

                            int playtime = (int) Database.getTotalPlaytime(p.uuid());
                            String message = Bundle.format("commands.rank.next-rank.info", locale,
                                    nextRank.formatted(p),
                                    targetColor(record.getAttacks(), nextRank.requirements.attacks), record.getAttacks(), nextRank.requirements.attacks,
                                    targetColor(record.getWaves(), nextRank.requirements.waves), record.getWaves(), nextRank.requirements.waves,
                                    targetColor(record.getHexesCaptured(), nextRank.requirements.hexes), record.getHexesCaptured(), nextRank.requirements.hexes,
                                    targetColor(record.getBuilt(), nextRank.requirements.built), record.getBuilt(), nextRank.requirements.built,
                                    targetColor(playtime, nextRank.requirements.playtime * 60), longToTime(playtime, locale), longToTime(nextRank.requirements.playtime * 60, locale)
                            );
                            Call.menu(p.con(), 0, Bundle.get("menus.rank-info.title", locale), message, newButtons);
                        }
                    } catch (SQLException e) {
                        Log.err(e);
                        Bundle.bundled(p, "commands.rank.error");
                    }
                }));
            } catch (SQLException e) {
                Bundle.bundled(player, "commands.rank.error");
                Log.err(e);
            }
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

                try {
                    String[][] newButtons = new String[][] {
                            {Bundle.get("menus.close", locale)}
                    };

                    Rank rank = Rank.values()[option];
                    StatsRecord record = Database.getStats(p.uuid());
                    int playtime = (int) Database.getTotalPlaytime(p.uuid());
                    String message = Bundle.format("commands.ranks.rank-info", locale,
                            rank.formatted(p),
                            targetColor(record.getAttacks(), rank.requirements.attacks), record.getAttacks(), rank.requirements.attacks,
                            targetColor(record.getWaves(), rank.requirements.waves), record.getWaves(), rank.requirements.waves,
                            targetColor(record.getHexesCaptured(), rank.requirements.hexes), record.getHexesCaptured(), rank.requirements.hexes,
                            targetColor(record.getBuilt(), rank.requirements.built), record.getBuilt(), rank.requirements.built,
                            targetColor(playtime, rank.requirements.playtime * 60), longToTime(playtime, locale), longToTime(rank.requirements.playtime * 60, locale)
                    );
                    Call.menu(p.con(), 0, Bundle.get("menus.rank-info.title", locale), message, newButtons);
                } catch (SQLException e) {
                    Log.err(e);
                    Bundle.bundled(p, "commands.rank.error");
                }
            });
        });

        commandHandler.<Player>register("stats", "commands.stats.description", (args, player) -> {
            try {
                UsersRecord record = Database.getPlayer(player.uuid());
                StatsRecord statsRecord = Database.getStats(player.uuid());
                long playtime = Database.getTotalPlaytime(player.uuid());
                Rank rank = Rank.getRank(player);
                Locale locale = Bundle.findLocale(player.locale);
                String message = Bundle.format("commands.stats.msg",
                        Bundle.findLocale(player.locale()), record.getId(), player.coloredName(), rank.formatted(player),
                        statsRecord.getBuilt(), statsRecord.getBroken(),
                        statsRecord.getAttacks(), statsRecord.getHexesCaptured(), statsRecord.getWaves(),
                        statsRecord.getLogins(), statsRecord.getMessages(), statsRecord.getDeaths(), StringUtils.longToTime(playtime, locale));
                String[][] buttons = {
                        {Bundle.get("commands.rank.next-rank", locale)},
                        {Bundle.get("menus.close", locale)}
                };
                MenuHandler.send(player, Bundle.get("menus.stats.title", locale), message, buttons, (menuId, option, p) -> {
                    if (option == -1 || option == 1) {
                        return;
                    }

                    try {
                        Rank nextRank = Rank.getRank(p).getNext();
                        String[][] newButtons = new String[][] {
                                {Bundle.get("menus.close", locale)}
                        };

                        if (nextRank == null) {
                            Call.menu(p.con(), 0, Bundle.get("menus.rank-info.title", locale), Bundle.format("commands.rank.next-rank.none", locale), newButtons);
                        } else {
                            String msg = Bundle.format("commands.rank.next-rank.info", locale,
                                    nextRank.formatted(p),
                                    targetColor(statsRecord.getAttacks(), nextRank.requirements.attacks), statsRecord.getAttacks(), nextRank.requirements.attacks,
                                    targetColor(statsRecord.getWaves(), nextRank.requirements.waves), statsRecord.getWaves(), nextRank.requirements.waves,
                                    targetColor(statsRecord.getHexesCaptured(), nextRank.requirements.hexes), statsRecord.getHexesCaptured(), nextRank.requirements.hexes,
                                    targetColor(statsRecord.getBuilt(), nextRank.requirements.built), statsRecord.getBuilt(), nextRank.requirements.built,
                                    targetColor((int) playtime, nextRank.requirements.playtime * 60), longToTime(playtime, locale), longToTime(nextRank.requirements.playtime * 60, locale)
                            );
                            Call.menu(p.con(), 0, Bundle.get("menus.rank-info.title", locale), msg, newButtons);
                        }
                    } catch (SQLException e) {
                        Log.err(e);
                        Bundle.bundled(p, "commands.rank.error");
                    }
                });
            } catch (SQLException e) {
                Log.err(e);
                Bundle.bundled(player, "commands.stats.error");
            }
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

            new Thread(() -> {
                boolean playerDetailed = false;
                boolean targetDetailed = false;
                try {
                    playerDetailed = Database.getPlayer(player.uuid()).getTranslator().equals("double");
                    targetDetailed = Database.getPlayer(player.uuid()).getTranslator().equals("double");
                    Database.createMessage(Const.serverFieldName, player.uuid(), target.uuid(), MessageType.direct, args[1], player.locale());
                } catch (SQLException e) {
                    Log.err(e);
                }

                Bundle.bundled(player, "commands.msg.to", Strings.stripColors(target.name()), String.format(playerDetailed ? "%s (%s)" : "%s", args[1], args[1]));
                Bundle.bundled(target, "commands.msg.from", Strings.stripColors(player.name()), String.format(targetDetailed ? "%s (%s)" : "%s", Translator.translateRaw(player, target, args[1]), args[1]));
            }).start();
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
                if (!jsallowed.containsKey(player.uuid())) {
                    Bundle.bundled(player, "commands.access-denied");
                    return;
                }
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
