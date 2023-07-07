package stellar.command;

import arc.Core;
import arc.struct.ObjectMap;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Strings;
import mindustry.content.Fx;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration;
import mindustry.net.Packets;
import net.dv8tion.jda.api.entities.MessageEmbed;
import stellar.Const;
import stellar.Variables;
import stellar.bot.Bot;
import stellar.bot.Colors;
import stellar.bot.Util;
import stellar.database.Database;
import stellar.database.enums.ServerEventTypes;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.BansRecord;
import stellar.database.gen.tables.records.ServerEventsRecord;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.util.Bundle;
import stellar.util.Players;
import stellar.util.StringUtils;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Base64;

import static mindustry.Vars.netServer;
import static stellar.Variables.config;

public class ServerCommands {

    public static void load(CommandHandler commandHandler) {
        commandHandler.removeCommand("exit");
        commandHandler.register("exit", "Exit the server application", args -> {
            Log.info("Shutting down server...");
            Groups.player.each(e -> e.kick(Packets.KickReason.serverClose));

            MessageEmbed embed = Util.embedBuilder("**Сервер остановлен**", Colors.red);
            try {
                ServerEventsRecord record = Database.getContext().newRecord(Tables.SERVER_EVENTS);
                record.setServer(Const.SERVER_COLUMN_NAME);
                record.setTimestamp(System.currentTimeMillis() / 1000);
                record.setType(ServerEventTypes.STOP.name());
                record.store();
            } catch (SQLException e) {
                Log.err(e);
            }

            Bot.sendEmbed(embed);
            Bot.shutdown();
            Core.app.exit();
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
                Log.info("RTV: некорректное действие");
            }
            Log.info(rtvEnabled ? "RTV включен" : "RTV выключен");
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

                BansRecord bansRecord = Database.getContext().newRecord(Tables.BANS); // move into separate method
                bansRecord.setAdmin("console");
                bansRecord.setTarget(record.getUuid());
                bansRecord.setCreated(LocalDateTime.now());
                if (period > -1) { bansRecord.setUntil(LocalDateTime.now().plusDays(period)); }
                bansRecord.setReason(reason);
                bansRecord.store();

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
                MessageEmbed banEmbed = Util.embedBuilder("Бан (через консоль)", message, Colors.red, LocalDateTime.now(), Const.SERVER_COLUMN_NAME);
                Bot.sendEmbed(config.bot.bansId, banEmbed);

                Log.info("Player @ / @ / #@ got banned", Strings.stripColors(record.getName()), record.getUuid(), record.getId());
            } catch (SQLException e) {
                Log.err(e);
            }
        });

        commandHandler.removeCommand("unban");
        commandHandler.register("unban", "<id/uuid>", "Unban a player.", args -> {
            Log.err("[WIP]");
        });
    }

}
