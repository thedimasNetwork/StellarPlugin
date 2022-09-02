package stellar.command;

import arc.Core;
import arc.struct.ObjectMap;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.net.Administration;
import stellar.Const;
import stellar.PlayerData;
import stellar.ThedimasPlugin;
import stellar.database.DBHandler;

import java.sql.SQLException;
import java.text.MessageFormat;

import static mindustry.Vars.netServer;

public class ServerCommands {
    public static void load(CommandHandler handler) {
        ThedimasPlugin plugin = (ThedimasPlugin)Vars.mods.getMod(Const.PLUGIN_NAME).main;
        handler.register("export-players", "Export players into DB", args -> {
            ObjectMap<String, Administration.PlayerInfo> playerList = Reflect.get(netServer.admins, "playerInfo");
            int exported = 0;
            for (Administration.PlayerInfo info : playerList.values()) {
                PlayerData data = new PlayerData();
                data.uuid = info.id;
                data.ip = info.lastIP;
                data.name = info.lastName;
                data.admin = info.admin;
                data.banned = info.banned;

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

        handler.register("auto-pause", "[on|off]", "Pause game with 0 people online", args -> {
            if (args.length > 0 && (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
                plugin.autoPause = args[0].equalsIgnoreCase("on") || !args[0].equalsIgnoreCase("off");
                if (Groups.player.size() < 1 && plugin.autoPause) {
                    Vars.state.serverPaused = true;
                    Log.info("auto-pause: @ игроков онлайн -> Игра поставлена на паузу...", Groups.player.size());
                } else if (!plugin.autoPause) {
                    Vars.state.serverPaused = false;
                    Log.info("auto-pause: @ игрок(ов) онлайн -> Игра снята с паузы...", Groups.player.size());
                }
                return;
            } else if (args.length > 0) {
                Log.info("auto-pause: некорректное действие");
            }
            Log.info(plugin.autoPause ? "Авто-пауза включена" : "Авто-пауза выключена");
        });

        handler.register("rtv", "[on|off]", "disable or enable RTV", args -> {
            if (args.length > 0 && (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
                boolean enabled = args[0].equalsIgnoreCase("on") || !args[0].equalsIgnoreCase("off");
                plugin.rtv = enabled;
                Core.settings.put("rtv", enabled);
                if (!plugin.rtv && plugin.votesRTV.size() > 0) {
                    plugin.votesRTV.clear();
                    ThedimasPlugin.bundled("commands.rtv.votes-clear");
                }
            } else if (args.length > 0) {
                Log.info("RTV: некорректное действие");
            }
            Log.info(plugin.rtv ? "RTV включен" : "RTV выключен");
        });
    }
}
