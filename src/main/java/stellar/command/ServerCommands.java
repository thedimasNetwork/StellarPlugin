package stellar.command;

import arc.Core;
import arc.struct.ObjectMap;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Strings;
import mindustry.content.Fx;
import mindustry.gen.Groups;
import mindustry.net.Administration;
import mindustry.net.Packets;
import net.dv8tion.jda.api.entities.MessageEmbed;
import stellar.Const;
import stellar.Variables;
import stellar.bot.Bot;
import stellar.bot.Colors;
import stellar.bot.Util;
import stellar.database.DBHandler;
import stellar.database.entries.PlayerEntry;
import stellar.database.entries.PlaytimeEntry;
import stellar.database.entries.ServerEventEntry;
import stellar.database.enums.ServerEventTypes;
import stellar.database.tables.Tables;
import stellar.util.Bundle;

import java.sql.SQLException;
import java.text.MessageFormat;

import static mindustry.Vars.netServer;

public class ServerCommands {

    public static void load(CommandHandler commandHandler) {
        commandHandler.removeCommand("exit");
        commandHandler.register("exit", "Exit the server application", args -> {
            Log.info("Shutting down server...");
            Groups.player.each(e -> e.kick(Packets.KickReason.serverClose));

            MessageEmbed embed = Util.embedBuilder("*Сервер остановлен*", Colors.red);

            ServerEventEntry entry = ServerEventEntry.builder()
                    .server(Const.SERVER_COLUMN_NAME)
                    .timestamp((int) (System.currentTimeMillis() / 1000))
                    .type(ServerEventTypes.STOP)
                    .build();
            try {
                DBHandler.save(entry, Tables.serverEvents);
            } catch (SQLException e) {
                Log.err(e);
            }

            Bot.sendEmbed(embed);
            Bot.shutdown();
            Core.app.exit();
        });

        commandHandler.register("export-players", "Export players into DB", args -> {
            ObjectMap<String, Administration.PlayerInfo> playerList = Reflect.get(netServer.admins, "playerInfo");
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
                    if (!DBHandler.userExist(info.id)) {
                        DBHandler.save(data, Tables.users);
                        DBHandler.save(PlaytimeEntry.builder().uuid(info.id).build(), Tables.playtime);
                        exported++;
                    }
                } catch (SQLException e) {
                    Log.err(e.getMessage());
                    Log.err("Unable to export data of player @ (@)", Strings.stripColors(info.lastName), info.id);
                }
            }
            Log.info(MessageFormat.format("Successfully exported {0} players", exported));
        });

        commandHandler.register("rtv", "[on|off]", "disable or enable RTV", args -> {
            boolean rtvEnabled = Core.settings.getBool("rtv");

            if (args.length > 0 && (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
                rtvEnabled = args[0].equalsIgnoreCase("on") || !args[0].equalsIgnoreCase("off");
                Core.settings.put("rtv", rtvEnabled);

                if (!rtvEnabled && Variables.votesRTV.size() > 0) {
                    Variables.votesRTV.clear();
                    Bundle.bundled("commands.rtv.votes-clear");
                }
            } else if (args.length > 0) {
                Log.info("RTV: некорректное действие");
            }
            Log.info(rtvEnabled ? "RTV включен" : "RTV выключен");
        });
        commandHandler.register("effect", "<effect>", "set new moving effect", args -> {
            try {
                Fx.class.getField(args[0]).get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                Log.err("Effect @ not found", args[0]);
                return;
            }
            Core.settings.put("effect", args[0]);
        });
    }

}
