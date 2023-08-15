package stellar.plugin.commands;

import arc.Core;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import mindustry.content.Fx;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Packets;
import net.dv8tion.jda.api.entities.MessageEmbed;
import stellar.database.Database;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.components.bot.Bot;
import stellar.plugin.components.bot.Colors;
import stellar.plugin.components.bot.Util;
import thedimas.util.Bundle;
import stellar.plugin.util.Players;
import stellar.plugin.util.StringUtils;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static stellar.plugin.Variables.config;

public class ServerCommands {

    public static void load(CommandHandler commandHandler) {
        commandHandler.removeCommand("exit");
        commandHandler.register("exit", "Exit the server application", args -> {
            Log.info("Shutting down server...");
            Groups.player.each(e -> e.kick(Packets.KickReason.serverClose));

            MessageEmbed embed = Util.embedBuilder("**Сервер остановлен**", Colors.red);
            Bot.sendEmbed(embed);
            Core.app.exit();
            Core.app.post(Bot::shutdown);
        });

        commandHandler.register("export-players", "Export players into DB", args -> {
            /*ObjectMap<String, Administration.PlayerInfo> playerList = Reflect.get(netServer.admins, "playerInfo");
            int exported = 0;
            for (Administration.PlayerInfo info : playerList.values()) {
                PlayerEntry data = PlayerEntry.builder()
                        .uuid(info.id)
                        .ip(info.lastIP)
                        .name(info.lastName)
                        .admin(info.admin)
                        .banned(info.banned)
                        .build();

                try {
                    if (!Database.userExist(info.id)) {
                        Database.save(data, Tables.users);
                        Database.save(PlaytimeEntry.builder().uuid(info.id).build(), Tables.playtime);
                        exported++;
                    }
                } catch (SQLException e) {
                    Log.err(e.getMessage());
                    Log.err("Unable to export data of player @ (@)", Strings.stripColors(info.lastName), info.id);
                }
            }
            Log.info(MessageFormat.format("Successfully exported {0} players", exported));*/
            Log.warn("Command was removed because of changes in DB structure. Maybe once it will be working again...");
        });

        commandHandler.register("rtv", "[on|off]", "Disable or enable RTV.", args -> {
            boolean rtvEnabled = Core.settings.getBool("rtv");

            if (args.length > 0 && (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
                rtvEnabled = args[0].equalsIgnoreCase("on") || !args[0].equalsIgnoreCase("off");
                Core.settings.put("rtv", rtvEnabled);

                if (!rtvEnabled && Variables.votesRTV.size > 0) {
                    Variables.votesRTV.clear();
                    Bundle.bundled("commands.rtv.votes-clear");
                }
            } else if (args.length > 0) {
                Log.info("RTV: incorrect action");
            }
            Log.info(rtvEnabled ? "RTV enabled" : "RTV disabled");
        });

        commandHandler.register("effect", "<effect>", "Set new moving effect.", args -> {
            try {
                Fx.class.getField(args[0]).get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                Log.err("Effect @ not found", args[0]);
                return;
            }
            Core.settings.put("effect", args[0]);
        });

        commandHandler.removeCommand("ban");
        commandHandler.register("ban", "<id/uuid/name> [period] [reason...]", "Ban a player. Use underscores instead of spaces for usernames.", args -> {
            try {
                UsersRecord record = null;
                if (Strings.canParseInt(args[0])) {
                    record = Database.getPlayer(Strings.parseInt(args[0]));
                    Log.debug("@ - id", record == null);
                }
                if (StringUtils.isBase64(args[0]) && record == null) {
                    record = Database.getPlayer(args[0]);
                    Log.debug("@ - uuid", record == null);
                }
                if (record == null) {
                    Player player = Players.findPlayer(args[0]);
                    if (player != null) {
                        record = Database.getPlayer(player.uuid());
                    }
                    Log.debug("@ - last (username)", record == null);
                }

                if (record == null) {
                    Log.err("Player @ not found", args[0]);
                    return;
                }

                if (Database.isBanned(record.getUuid())) {
                    Log.err("Player @ is already banned", Strings.stripColors(record.getName()));
                    return;
                }

                int period = -1;
                String reason = args.length > 2 ? args[2] : "<не указана>";
                if (args.length > 1) {
                    if (Strings.canParseInt(args[1])) {
                        period = Strings.parseInt(args[1]);
                    } else {
                        Log.err("Invalid period!");
                        return;
                    }
                }

                Player player = Players.getPlayer(record.getUuid());
                if (player != null) {
                    player.kick(Packets.KickReason.banned);
                }

                Database.ban("console", record.getUuid(), period, reason);
                String message = """
                        **Админ**: `<консоль>`
                        **Нарушитель**: %target% (%tid%)
                        **Причина**: %reason%
                        """.replace("%target%", Strings.stripColors(record.getName())).replace("%tid%", record.getId().toString())
                        .replace("%reason%", reason);
                if (period > -1) {
                    message += "**Срок**: <t:%timestamp%:f>".replace("%timestamp%", (System.currentTimeMillis() / 1000 + period * (24 * 60 * 60)) + "");
                } else {
                    message += "**Срок**: Перманентный";
                }
                MessageEmbed banEmbed = Util.embedBuilder("Бан (через консоль)", message, Colors.red, LocalDateTime.now(), Const.serverFieldName);
                Bot.sendEmbed(config.bot.bansId, banEmbed);

                Log.info("Player @ / @ / #@ got banned", Strings.stripColors(record.getName()), record.getUuid(), record.getId());
            } catch (SQLException e) {
                Log.err(e);
            }
        });

        commandHandler.removeCommand("unban");
        commandHandler.register("unban", "<id/uuid>", "Unban a player.", args -> {
            try {
                UsersRecord record = null;
                if (Strings.canParseInt(args[0])) {
                    record = Database.getPlayer(Strings.parseInt(args[0]));
                } else if (StringUtils.isBase64(args[0])) {
                    record = Database.getPlayer(args[0]);
                }

                if (record == null) {
                    Log.err("Player not found");
                    return;
                }

                if (!Database.isBanned(record.getUuid())) {
                    Log.err("Player @ is not banned", Strings.stripColors(record.getName()));
                    return;
                }

                Database.unban(record.getUuid());
                Log.info("Player @ / @ / #@ got unbanned", Strings.stripColors(record.getName()), record.getUuid(), record.getId());

                String message = """
                        **Админ**: `<консоль>`
                        **Нарушитель**: %target% (%tid%)
                        """.replace("%target%", Strings.stripColors(record.getName())).replace("%tid%", record.getId().toString());
                MessageEmbed unbanEmbed = Util.embedBuilder("Разбан (через консоль)", message, Colors.green, LocalDateTime.now());
                Bot.sendEmbed(config.bot.bansId, unbanEmbed);
            } catch (IllegalArgumentException | SQLException e) {
                Log.err(e);
            }
        });

        commandHandler.register("corestats", "Get current player stats.", args -> {
            Variables.statsData.each((uuid, stats) -> {
                Log.info("@:", uuid);
                stats.each((name, value) -> {
                    Log.info(" - @: @", name, value);
                });
            });
        });

        commandHandler.register("restart", "Restart the server", args -> {
            Groups.player.each(player -> {
                player.kick(Packets.KickReason.serverRestarting);
            });
            MessageEmbed embed = Util.embedBuilder("**Сервер перезапускается**", Colors.yellow);
            Bot.sendEmbed(embed);
            System.exit(2);
        });
    }
}
