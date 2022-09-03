package stellar.command;

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
import stellar.Const;
import stellar.ThedimasPlugin;
import stellar.Variables;
import stellar.database.DBHandler;
import stellar.database.tables.Playtime;
import stellar.database.tables.Users;
import stellar.history.entry.HistoryEntry;
import stellar.history.struct.CacheSeq;
import stellar.util.Bundle;
import stellar.util.Translator;
import stellar.util.logger.DiscordLogger;

import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.locales;
import static mindustry.Vars.world;


public class PlayerCommands {

    public static void load(CommandHandler commandHandler) {
        commandHandler.removeCommand("t");
        commandHandler.<Player>register("t", "<text...>", "commands.t.description", (args, player) -> {
            String message = args[0];
            Groups.player.each(o -> o.team() == player.team(), otherPlayer -> {
                String msg = Translator.translateChat(player, otherPlayer, message);
                otherPlayer.sendMessage("<[#" + player.team().color + "]T[]>" + msg);
            });

            Log.info("<T>" + Const.CHAT_LOG_FORMAT, Strings.stripColors(player.name), Strings.stripColors(message), player.locale);
        });

        commandHandler.removeCommand("help");
        commandHandler.<Player>register("help", "[page]", "commands.help.description", (args, player) -> {
            if (args.length > 0 && !Strings.canParseInt(args[0])) {
                ThedimasPlugin.bundled(player, "commands.page-not-int");
                return;
            }

            Locale locale = ThedimasPlugin.findLocale(player.locale);
            int hiddenCommandsCount = player.admin ? 0 : commandHandler.getCommandList().count(c -> c.description.startsWith("commands.admin"));
            int pages = Mathf.ceil(commandHandler.getCommandList().size / Const.LIST_PAGE_SIZE - hiddenCommandsCount / Const.LIST_PAGE_SIZE);
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;

            if (--page >= pages || page < 0) {
                ThedimasPlugin.bundled(player, "commands.under-page", pages);
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
                locale = DBHandler.get(player.uuid(), Users.TRANSLATOR);
            } catch (Throwable t) {
                ThedimasPlugin.bundled(player, "commands.tr.error");
                Log.err(t);
                DiscordLogger.err(t);
                return;
            }

            if (args.length == 0) {
                ThedimasPlugin.bundled(player, "commands.tr.current", locale);
                return;
            }

            String mode = args[0].toLowerCase();
            switch (mode) {
                case "off" -> {
                    try {
                        DBHandler.update(player.uuid(), Users.TRANSLATOR, "off");
                    } catch (Throwable t) {
                        Log.err(t);
                        DiscordLogger.err(t);
                    }
                    ThedimasPlugin.bundled(player, "commands.tr.disabled");
                }
                case "auto" -> {
                    try {
                        DBHandler.update(player.uuid(), Users.TRANSLATOR, "auto");
                    } catch (Throwable t) {
                        Log.err(t);
                        DiscordLogger.err(t);
                    }
                    ThedimasPlugin.bundled(player, "commands.tr.auto");
                }
                case "double" -> {
                    try {
                        DBHandler.update(player.uuid(), Users.TRANSLATOR, "double");
                    } catch (Throwable t) {
                        Log.err(t);
                        DiscordLogger.err(t);
                    }
                    ThedimasPlugin.bundled(player, "commands.tr.double");
                }
                default -> {
                    Locale target = Structs.find(locales, l -> mode.equalsIgnoreCase(l.toString()));
                    if (target == null) {
                        ThedimasPlugin.bundled(player, "commands.tr.list", Const.LocaleListHolder.LOCALE_LIST);
                        return;
                    }
                    try {
                        DBHandler.update(player.uuid(), Users.TRANSLATOR, target.toString());
                    } catch (Throwable t) {
                        Log.err(t);
                        DiscordLogger.err(t);
                    }
                    ThedimasPlugin.bundled(player, "commands.tr.set", target);
                }
            }
        });

        commandHandler.<Player>register("rtv", "[on|off]", "commands.rtv.description", (args, player) -> {
            boolean rtvEnabled = Core.settings.getBool("rtv");

            if (args.length > 0) {
                if (!Variables.admins.containsKey(player.uuid())) {
                    ThedimasPlugin.bundled(player, "commands.rtv.access-denied");
                    return;
                }
                rtvEnabled = !args[0].equalsIgnoreCase("off");
                Core.settings.put("rtv", rtvEnabled);
                if (!rtvEnabled && Variables.votesRTV.size() > 0) {
                    Variables.votesRTV.clear();
                    ThedimasPlugin.bundled("commands.rtv.votes-clear");
                }
            }

            if (!rtvEnabled) {
                ThedimasPlugin.bundled(player, "commands.rtv.disabled");
                return;
            }

            Variables.votesRTV.add(player.uuid());
            int cur = Variables.votesRTV.size();
            int req = (int) Math.ceil(Const.VOTES_RATIO * Groups.player.size());

            String playerName = player.coloredName();
            ThedimasPlugin.bundled("commands.rtv.vote", playerName, cur, req);

            if (cur >= req) {
                Variables.votesRTV.clear();
                ThedimasPlugin.bundled("commands.rtv.passed");
                Events.fire(new EventType.GameOverEvent(Team.derelict));
            }
        });

        commandHandler.<Player>register("version", "commands.version.description", (arg, player) -> ThedimasPlugin.bundled(player, "commands.version.msg", Const.PLUGIN_VERSION));

        commandHandler.<Player>register("discord", "commands.discord.description", (args, player) -> Call.openURI(player.con, "https://discord.gg/RkbFYXFU9E"));

        commandHandler.<Player>register("rules", "commands.rules.description", (args, player) -> ThedimasPlugin.bundled(player, "rules"));

        commandHandler.<Player>register("hub", "commands.hub.description", (args, player) -> {
            String[] address = Const.SERVER_ADDRESS.get("hub").split(":");
            String ip = address[0];
            int port = Integer.parseInt(address[1]);

            Call.connect(player.con, ip, port);
        });

        commandHandler.<Player>register("connect", "[list|server...]", "commands.connect.description", (args, player) -> {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                ThedimasPlugin.bundled(player, "commands.connect.list", Const.SERVER_LIST);
                return;
            }

            String serverName = args[0].toLowerCase();
            if (!Const.SERVER_ADDRESS.containsKey(serverName)) {
                ThedimasPlugin.bundled(player, "commands.server-notfound", Const.SERVER_LIST);
                return;
            }

            String[] address = Const.SERVER_ADDRESS.get(serverName).split(":");
            String ip = address[0];
            int port = Integer.parseInt(address[1]);
            Vars.net.pingHost(ip, port, host -> Call.connect(player.con, ip, port), e -> ThedimasPlugin.bundled(player, "commands.connect.server-offline"));
        });

        commandHandler.<Player>register("history", "[page] [detailed]", "commands.history.description", (args, player) -> {
            boolean detailed = args.length == 2 && Structs.contains(Const.BOOL_VALUES.split(", "), args[1].toLowerCase());

            if (args.length > 0 && Variables.activeHistoryPlayers.containsKey(player.uuid())) {
                if (!Strings.canParseInt(args[0])) {
                    ThedimasPlugin.bundled(player, "commands.page-not-int");
                    return;
                }

                int mouseX = Mathf.clamp(Mathf.round(player.mouseX / 8), 1, world.width());
                int mouseY = Mathf.clamp(Mathf.round(player.mouseY / 8), 1, world.height());
                Locale locale = ThedimasPlugin.findLocale(player.locale);

                CacheSeq<HistoryEntry> entries = Variables.getHistorySeq(mouseX, mouseY);

                int page = Integer.parseInt(args[0]) - 1;
                int pages = Mathf.ceil(entries.size / Const.LIST_PAGE_SIZE);

                if (page >= pages || pages < 0 || page < 0) {
                    ThedimasPlugin.bundled(player, "commands.under-page", page);
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
                ThedimasPlugin.bundled(player, "commands.history.detailed.disabled");
            } else if (args.length == 2) {
                Variables.activeHistoryPlayers.put(player.uuid(), detailed);
                String msg = detailed ? "commands.history.detailed" : "commands.history.default";
                ThedimasPlugin.bundled(player, "commands.history.enabled", msg);
            } else {
                Variables.activeHistoryPlayers.put(player.uuid(), false);
                ThedimasPlugin.bundled(player, "commands.history.disabled");
            }
        });

        commandHandler.<Player>register("playtime", "[server...]", "commands.playtime.description", (args, player) -> {
            String serverColumnName;
            if (args.length > 0) {
                serverColumnName = args[0].toLowerCase();
            } else {
                serverColumnName = Const.SERVER_COLUMN_NAME;
            }

            if (!Playtime.FIELDS.containsKey(serverColumnName)) {
                ThedimasPlugin.bundled(player, "commands.server-notfound", Const.SERVER_LIST);
                return;
            }

            try {
                Long time = DBHandler.get(player.uuid(), Playtime.FIELDS.get(serverColumnName));
                if (time == null) {
                    Log.err("Player '" + player.uuid() + "' doesn't exists");
                }
                ThedimasPlugin.bundled(player, "commands.playtime.msg", Const.SERVER_NAMES.get(serverColumnName), longToTime(time != null ? time : 0L));
            } catch (Throwable t) {
                Log.err("Failed to get playtime for player '" + player.uuid() + "'", t);
            }
        });

        commandHandler.<Player>register("score", "commands.score.description", (args, player) -> {
            try {
                int exp = DBHandler.get(player.uuid(), Users.EXP);
                ThedimasPlugin.bundled(player, "commands.score.msg", exp);
            } catch (SQLException e) {
                Log.err(e);
            }
        });
    }

    private static String longToTime(long seconds) {
        long min = seconds / 60;
        long hour = min / 60;
        return String.format("%d:%02d:%02d", hour, min % 60, seconds % 60);
    }

}
