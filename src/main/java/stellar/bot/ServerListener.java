package stellar.bot;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType;
import stellar.ThedimasPlugin;

public class ServerListener {
    public static void listen() {
        Events.on(EventType.PlayerJoin.class, event -> {
            Bot.sendMessage(String.format("%s зашел на сервер", ThedimasPlugin.stripColorsAndGlyphs(event.player.name)));
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            Bot.sendMessage(String.format("%s покинул сервер", ThedimasPlugin.stripColorsAndGlyphs(event.player.name)));
        });
    }
}
