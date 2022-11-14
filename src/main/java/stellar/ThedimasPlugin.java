package stellar;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.mod.Plugin;
import stellar.bot.Bot;
import stellar.command.AdminCommands;
import stellar.command.PlayerCommands;
import stellar.command.ServerCommands;
import stellar.components.EventHandler;
import stellar.components.Experience;
import stellar.components.LaunchPad;
import stellar.history.History;

import static mindustry.Vars.netServer;

@SuppressWarnings({"unused"})
public class ThedimasPlugin extends Plugin {
    @Override
    public void init() {
        Log.info("ThedimasPlugin launched!");
        netServer.admins.addChatFilter((player, message) -> null);

        Config.load();
        Bot.load();
        Experience.load();
        LaunchPad.load();
        EventHandler.load();
        History.load();
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
