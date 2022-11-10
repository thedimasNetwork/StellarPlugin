package stellar;

import arc.Core;
import arc.util.*;
import mindustry.mod.Plugin;

import stellar.bot.Bot;
import stellar.command.*;
import stellar.components.EventHandler;
import stellar.components.Experience;
import stellar.components.LaunchPad;
import stellar.history.History;

import static mindustry.Vars.*;

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
