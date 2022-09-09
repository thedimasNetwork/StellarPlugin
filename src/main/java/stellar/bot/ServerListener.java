package stellar.bot;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import stellar.ThedimasPlugin;

public class ServerListener {
    public static void listen() {
        Events.on(EventType.PlayerJoin.class, event -> {
            String name = ThedimasPlugin.stripColorsAndGlyphs(event.player.name);
            MessageEmbed embed = new EmbedBuilder()
                    .setDescription(String.format("%s зашел на сервер", name))
                    .setColor(Colors.green)
                    .build();
            Bot.sendEmbed(embed);
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            String name = ThedimasPlugin.stripColorsAndGlyphs(event.player.name);
            MessageEmbed embed = new EmbedBuilder()
                    .setDescription(String.format("%s покинул сервер", name))
                    .setColor(Colors.red)
                    .build();
            Bot.sendEmbed(embed);
        });

        Events.on(EventType.PlayerChatEvent.class, event -> {
            if (event.message.startsWith("/")) {
                return;
            }
            String name = ThedimasPlugin.stripColorsAndGlyphs(event.player.name);
            String text = ThedimasPlugin.stripColorsAndGlyphs(event.message);
            String msg = String.format("**%s**: %s", name, text);
            Bot.sendMessage(msg);
        });
    }
}
