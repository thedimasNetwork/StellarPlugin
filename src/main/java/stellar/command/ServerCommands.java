package stellar.command;

import arc.Core;
import arc.struct.ObjectMap;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Strings;
import mindustry.gen.Groups;
import mindustry.net.Administration;
import mindustry.net.Packets;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import stellar.database.PlayerData;
import stellar.Variables;
import stellar.bot.Bot;
import stellar.bot.Colors;
import stellar.database.DBHandler;
import stellar.util.Bundle;

import java.sql.SQLException;
import java.text.MessageFormat;

import static mindustry.Vars.netServer;
import static mindustry.Vars.state;

public class ServerCommands {

    public static void load(CommandHandler commandHandler) {
        commandHandler.removeCommand("exit");
        commandHandler.register("exit", "Exit the server application", args -> {
            Log.info("Shutting down server...");
            Groups.player.each(e -> e.kick(Packets.KickReason.serverClose));

            MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Сервер остановлен")
                    .setColor(Colors.red)
                    .build();
            Bot.sendEmbed(embed);
            Bot.shutdown();
            Core.app.exit();
        });

        commandHandler.register("export-players", "Export players into DB", args -> {
            ObjectMap<String, Administration.PlayerInfo> playerList = Reflect.get(netServer.admins, "playerInfo");
            int exported = 0;
            for (Administration.PlayerInfo info : playerList.values()) {
                PlayerData data = PlayerData.builder()
                        .uuid(info.id)
                        .ip(info.lastIP)
                        .name(info.lastName)
                        .admin(info.admin)
                        .banned(info.banned)
                        .build();

                try {
                    if (!DBHandler.userExist(info.id)) {
                        DBHandler.save(data);
                        exported++;
                    }
                } catch (SQLException e) {
                    Log.err(e.getMessage());
                    Log.err("Unable to export data of player @ (@)", Strings.stripColors(info.lastName), info.id);
                }
            }
            Log.info(MessageFormat.format("Successfully exported {0} players", exported));
        });

        commandHandler.register("auto-pause", "[on|off]", "Pause game with 0 people online", args -> {
            boolean autoPauseEnabled = Core.settings.getBool("autoPause");

            if (args.length > 0 && (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
                autoPauseEnabled = args[0].equalsIgnoreCase("on") || !args[0].equalsIgnoreCase("off");
                Core.settings.put("autoPause", autoPauseEnabled);

                if (Groups.player.size() < 1 && autoPauseEnabled) {
                    state.serverPaused = true;
                    Log.info("auto-pause: @ игроков онлайн -> Игра поставлена на паузу...", Groups.player.size());
                } else if (!autoPauseEnabled) {
                    state.serverPaused = false;
                    Log.info("auto-pause: @ игрок(ов) онлайн -> Игра снята с паузы...", Groups.player.size());
                }
                return;
            } else if (args.length > 0) {
                Log.info("auto-pause: некорректное действие");
            }
            Log.info(autoPauseEnabled ? "Авто-пауза включена" : "Авто-пауза выключена");
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
    }

}
