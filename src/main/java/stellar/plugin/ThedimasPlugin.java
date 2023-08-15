package stellar.plugin;

import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Time;
import arc.util.Timer;
import mindustry.mod.Plugin;
import stellar.database.Database;
import stellar.plugin.components.bot.Bot;
import stellar.plugin.commands.AdminCommands;
import stellar.plugin.commands.PlayerCommands;
import stellar.plugin.commands.ServerCommands;
import stellar.plugin.components.Activity;
import stellar.plugin.components.AntiVPN;
import stellar.plugin.components.EventHandler;
import stellar.plugin.components.LaunchPad;
import stellar.plugin.components.history.History;

import java.util.concurrent.CompletableFuture;

import static mindustry.Vars.netServer;
import static stellar.plugin.Variables.commandManager;
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

        Timer.schedule(() -> {
            long start = Time.millis();
            CompletableFuture.runAsync(() -> {
                Log.warn("Collecting garbage");
                System.gc();
            }).thenRunAsync(() -> {
                Log.info("Garbage was collected in @ secs", (Time.millis() - start) / 1000f);
            });
        }, 600, 600);
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        ServerCommands.load(handler);
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        commandManager.setClientHandler(handler);
        PlayerCommands.load(handler);
        AdminCommands.load(handler);
    }
}
