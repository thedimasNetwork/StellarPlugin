package stellar.plugin;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.mod.Plugin;
import stellar.database.Database;
import stellar.plugin.bot.Bot;
import stellar.plugin.command.AdminCommands;
import stellar.plugin.command.PlayerCommands;
import stellar.plugin.command.ServerCommands;
import stellar.plugin.components.Activity;
import stellar.plugin.components.AntiVPN;
import stellar.plugin.components.EventHandler;
import stellar.plugin.components.LaunchPad;
import stellar.plugin.history.History;

import static mindustry.Vars.netServer;
import static stellar.plugin.Variables.config;

@SuppressWarnings({"unused"})
public class ThedimasPlugin extends Plugin {
    @Override
    public void init() {
        Log.info("ThedimasPlugin launched!");
        netServer.admins.addChatFilter((player, message) -> null);

        Config.load();
        Bot.load();
        Database.load(config.database.ip, config.database.port, config.database.name, config.database.user, config.database.password);
        LaunchPad.load();
        EventHandler.load();
        Activity.load();
        History.load();
        AntiVPN.load();
        Log.info("All components loaded");
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        ServerCommands.load(handler);
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        PlayerCommands.load(handler);
        AdminCommands.load(handler);
    }
}
