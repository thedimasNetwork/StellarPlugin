package stellar.plugin;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.mod.Plugin;
import stellar.plugin.bot.Bot;
import stellar.plugin.command.AdminCommands;
import stellar.plugin.command.PlayerCommands;
import stellar.plugin.command.ServerCommands;
import stellar.plugin.components.AntiVPN;
import stellar.plugin.components.EventHandler;
import stellar.plugin.components.LaunchPad;
import stellar.plugin.components.Playtime;
import stellar.plugin.history.History;

import static mindustry.Vars.netServer;

@SuppressWarnings({"unused"})
public class ThedimasPlugin extends Plugin {
    @Override
    public void init() {
        Log.info("ThedimasPlugin launched!");
        netServer.admins.addChatFilter((player, message) -> null);

        Config.load();
        Bot.load();
//        Experience.load(); // Work in progress on better version
        LaunchPad.load();
        EventHandler.load();
        Playtime.load();
        History.load();
        AntiVPN.load();
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
