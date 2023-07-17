package stellar.plugin.command;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
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
import java.text.MessageFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.*;
import static stellar.plugin.Variables.config;
import static stellar.plugin.Variables.jsallowed;
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

            Log.info("<T>" + Const.CHAT_LOG_FORMAT, Strings.stripColors(player.name), Strings.stripColors(message), player.locale);
        });

        commandHandler.removeCommand("help");
        commandHandler.<Player>register("help", "[page]", "commands.help.description", (args, player) -> {
            if (args.length > 0 && !Strings.canParseInt(args[0])) {
                Bundle.bundled(player, "commands.page-not-int");
                return;
            }

            Locale locale = Bundle.findLocale(player.locale);
            int hiddenCommandsCount = player.admin ? 0 : commandHandler.getCommandList().count(c -> c.description.startsWith("commands.admin"));
            int pages = Mathf.ceil(commandHandler.getCommandList().size / Const.LIST_PAGE_SIZE - hiddenCommandsCount / Const.LIST_PAGE_SIZE);
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
                locale = Database.getContext()
                        .select(Tables.USERS.TRANSLATOR)
                        .from(Tables.USERS)
                        .where(Tables.USERS.UUID.eq(player.uuid()))
                        .fetchOne().value1();
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
                        Database.getContext()
                                .update(Tables.USERS)
                                .set(Tables.USERS.TRANSLATOR, "off")
                                .where(Tables.USERS.UUID.eq(player.uuid()))
                                .execute();
                    } catch (Throwable t) {
                        Log.err(t);
                        DiscordLogger.err(t);
                    }
                    Bundle.bundled(player, "commands.tr.disabled");
                }
                case "auto" -> {
                    try {
                        Database.getContext()
                                .update(Tables.USERS)
                                .set(Tables.USERS.TRANSLATOR, "auto")
                                .where(Tables.USERS.UUID.eq(player.uuid()))
                                .execute();
                    } catch (Throwable t) {
                        Log.err(t);
                        DiscordLogger.err(t);
                    }
                    Bundle.bundled(player, "commands.tr.auto");
                }
                case "double" -> {
                    try {
                        Database.getContext()
                                .update(Tables.USERS)
                                .set(Tables.USERS.TRANSLATOR, "double")
                                .where(Tables.USERS.UUID.eq(player.uuid()))
                                .execute();
                    } catch (Throwable t) {
                        Log.err(t);
                        DiscordLogger.err(t);
                    }
                    Bundle.bundled(player, "commands.tr.double");
                }
                default -> {
                    Locale target = Structs.find(locales, l -> mode.equalsIgnoreCase(l.toString()));
                    if (target == null) {
                        Bundle.bundled(player, "commands.tr.list", Const.LocaleListHolder.LOCALE_LIST);
                        return;
                    }
                    try {
                        Database.getContext()
                                .update(Tables.USERS)
                                .set(Tables.USERS.TRANSLATOR, target.toString())
                                .where(Tables.USERS.UUID.eq(player.uuid()))
                                .execute();
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
            int req = (int) Math.ceil(Const.VOTES_RATIO * Groups.player.size());

            String playerName = player.coloredName();
            Bundle.bundled("commands.rtv.vote", playerName, cur, req);

            if (cur >= req) {
                Variables.votesRTV.clear();
                Bundle.bundled("commands.rtv.passed");
                Events.fire(new EventType.GameOverEvent(Team.derelict));
            }
        });

        commandHandler.<Player>register("version", "commands.version.description", (arg, player) -> Bundle.bundled(player, "commands.version.msg", Const.PLUGIN_VERSION));

        commandHandler.<Player>register("discord", "commands.discord.description", (args, player) -> Call.openURI(player.con, config.discordUrl));

        commandHandler.<Player>register("rules", "commands.rules.description", (args, player) -> Bundle.bundled(player, "rules"));

        commandHandler.<Player>register("hub", "commands.hub.description", (args, player) -> {
            String[] address = Const.SERVER_ADDRESS.get("hub").split(":");
            String ip = address[0];
            int port = Strings.parseInt(address[1]);

            Call.connect(player.con, ip, port);
        });

        commandHandler.<Player>register("connect", "[list|server...]", "commands.connect.description", (args, player) -> {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                Bundle.bundled(player, "commands.connect.list", Const.SERVER_LIST);
                return;
            }

            String serverName = args[0].toLowerCase();
            if (!Const.SERVER_ADDRESS.containsKey(serverName)) {
                Bundle.bundled(player, "commands.server-notfound", Const.SERVER_LIST);
                return;
            }

            String[] address = Const.SERVER_ADDRESS.get(serverName).split(":");
            String ip = address[0];
            int port = Strings.parseInt(address[1]);
            Vars.net.pingHost(ip, port, host -> Call.connect(player.con, ip, port), e -> Bundle.bundled(player, "commands.connect.server-offline"));
        });

        commandHandler.<Player>register("history", "[page] [detailed]", "commands.history.description", (args, player) -> {
            boolean detailed = args.length == 2 && Structs.contains(Const.BOOL_VALUES.split(", "), args[1].toLowerCase());

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
                int pages = Mathf.ceil(entries.size / Const.LIST_PAGE_SIZE);

                if (page >= pages || pages < 0 || page < 0) {
                    Bundle.bundled(player, "commands.under-page", page);
                    return;
                }

                StringBuilder result = new StringBuilder();
                result.append(Bundle.format("commands.history.page", locale, mouseX, mouseY, page + 1, pages)).append("\n");

                if (entries.isEmpty()) {
                    result.append(Bundle.get("history.empty", locale)).append("\n");
                }

                for (int i = 6 * page; i < Math.min(Const.LIST_PAGE_SIZE * (page + 1), entries.size); i++) {
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
                serverColumnName = Const.SERVER_COLUMN_NAME;
            }

            Log.debug(serverColumnName);
            Field<Long> field = (Field<Long>) Tables.PLAYTIME.field(serverColumnName);
            if (field == null) {
                Bundle.bundled(player, "commands.server-notfound", Const.SERVER_LIST);
                return;
            }

            try {
                Long time = Database.getContext()
                        .select(field)
                        .from(Tables.PLAYTIME)
                        .where(Tables.PLAYTIME.UUID.eq(player.uuid()))
                        .fetchOne().value1();
                Locale locale = Bundle.findLocale(player.locale());
                if (time == null) {
                    Log.err("Player '" + player.uuid() + "' doesn't exists");
                }
                Bundle.bundled(player, "commands.playtime.msg", Const.SERVER_NAMES.get(serverColumnName), StringUtils.longToTime((time == null) ? 0 : time, locale));
            } catch (Throwable t) {
                Log.err("Failed to get playtime for player '" + player.uuid() + "'", t);
            }
        });

        commandHandler.<Player>register("rank", "commands.rank.description", (args, player) -> {
            try {
                Rank rank = Rank.getRank(player);
                Locale locale = Bundle.findLocale(player.locale());
                String rankStr = rank.icon != null ?
                        String.format("<[#%s]%s[]> %s", rank.color, rank.icon, Bundle.get("ranks." + rank.name(), locale)) :
                        Bundle.get("ranks." + rank.name(), locale);

                String[][] buttons = {
                        {Bundle.get("commands.rank.next-rank", locale)},
                        {Bundle.get("menus.close", locale)}
                };

                MenuHandler.send(player, Bundle.get("menus.rank-info.title", locale), Bundle.format("commands.rank.msg", locale, rankStr), buttons, ((menuId, option, p) -> {
                    try {
                        Rank nextRank = Rank.getRank(p).getNext();
                        if (nextRank == null) {
                            Call.infoMessage(p.con(), Bundle.format("commands.rank.next-rank.none", locale));
                        } else {
                            UsersRecord record = Database.getPlayer(p.uuid());
                            int playtime = (int) Players.totalPlaytime(p.uuid());
                            String nextRankStr = nextRank.icon != null ?
                                    String.format("<[#%s]%s[]> %s", nextRank.color, nextRank.icon, Bundle.get("ranks." + nextRank.name(), locale)) :
                                    Bundle.get("ranks." + nextRank.name(), locale);

                            String message = Bundle.format("commands.rank.next-rank.info", locale,
                                    nextRankStr,
                                    targetColor(record.getAttacks(), nextRank.requirements.attacks), record.getAttacks(), nextRank.requirements.attacks,
                                    targetColor(record.getWaves(), nextRank.requirements.waves), record.getWaves(), nextRank.requirements.waves,
                                    targetColor(record.getHexes(), nextRank.requirements.hexes), record.getHexes(), nextRank.requirements.hexes,
                                    targetColor(record.getBuilt(), nextRank.requirements.built), record.getBuilt(), nextRank.requirements.built,
                                    targetColor(playtime, nextRank.requirements.playtime * 60), longToTime(playtime, locale), longToTime(nextRank.requirements.playtime * 60, locale)
                            );
                            Call.infoMessage(p.con(), message);
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
                 buttons[rank.ordinal()] = new String[]{rank.icon != null ?
                        String.format("<[#%s]%s[]> %s", rank.color, rank.icon, Bundle.get("ranks." + rank.name(), locale)) :
                        Bundle.get("ranks." + rank.name(), locale)};
            }
            buttons[Rank.values().length] = new String[]{Bundle.get("menus.close", locale)};

            MenuHandler.send(player, Bundle.get("menus.ranks-info.title", locale), "", buttons, (menuId, option, p) -> {
                if (option >= Rank.values().length || option < 0) {
                    return;
                }

                try {
                    Rank rank = Rank.values()[option];
                    UsersRecord record = Database.getPlayer(p.uuid());
                    int playtime = (int) Players.totalPlaytime(p.uuid());
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
                    Call.infoMessage(p.con(), message);
                } catch (SQLException e) {
                    Log.err(e);
                    Bundle.bundled(p, "commands.rank.error");
                }
            });
        });

        commandHandler.<Player>register("stats", "commands.stats.description", (args, player) -> {
            try {
                UsersRecord record = Database.getPlayer(player.uuid());
                long playtime = Players.totalPlaytime(player.uuid());
                Rank rank = Rank.getRank(player);
                Locale locale = Bundle.findLocale(player.locale);
                String rankStr = rank.icon != null ?
                        String.format("<[#%s]%s[]> %s", rank.color, rank.icon, Bundle.get("ranks." + rank.name(), locale)) :
                        Bundle.get("ranks." + rank.name(), locale);

                String message = Bundle.format("commands.stats.msg", Bundle.findLocale(player.locale()), record.getId(), player.coloredName(), rankStr, record.getBuilt(), record.getBroken(), record.getAttacks(), record.getHexes(), record.getWaves(), record.getLogins(), record.getMessages(), record.getDeaths(), StringUtils.longToTime(playtime, locale));
                String[][] buttons = {
                        {Bundle.get("commands.rank.next-rank", locale)},
                        {Bundle.get("menus.close", locale)}
                };
                MenuHandler.send(player, Bundle.get("menus.stats.title", locale), message, buttons, (menuId, option, p) -> {
                    try {
                        Rank nextRank = Rank.getRank(p).getNext();
                        if (nextRank == null) {
                            Call.infoMessage(p.con(), Bundle.format("commands.rank.next-rank.none", locale));
                        } else {
                            String nextRankStr = nextRank.icon != null ?
                                    String.format("<[#%s]%s[]> %s", nextRank.color, nextRank.icon, Bundle.get("ranks." + nextRank.name(), locale)) :
                                    Bundle.get("ranks." + nextRank.name(), locale);

                            String msg = Bundle.format("commands.rank.next-rank.info", locale,
                                    rankStr,
                                    targetColor(record.getAttacks(), nextRank.requirements.attacks), record.getAttacks(), nextRank.requirements.attacks,
                                    targetColor(record.getWaves(), nextRank.requirements.waves), record.getWaves(), nextRank.requirements.waves,
                                    targetColor(record.getHexes(), nextRank.requirements.hexes), record.getHexes(), nextRank.requirements.hexes,
                                    targetColor(record.getBuilt(), nextRank.requirements.built), record.getBuilt(), nextRank.requirements.built,
                                    targetColor((int) playtime, nextRank.requirements.playtime * 60), longToTime(playtime, locale), longToTime(nextRank.requirements.playtime * 60, locale)
                            );
                            Call.infoMessage(p.con(), msg);
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
            commandHandler.<Player>register("", "[some-text...]", "Test menu", (args, player) -> {
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
        }
        // endregion
    }
}
