package stellar.plugin.bot;

import arc.Events;
import mindustry.game.EventType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import stellar.plugin.util.StringUtils;

public class ServerListener {
    public static void listen() {
        Events.on(EventType.ServerLoadEvent.class, event -> {
            MessageEmbed embed = Util.embedBuilder("**Сервер запущен**", Colors.green);
            Bot.sendEmbed(embed);
        });

        Events.on(EventType.WorldLoadEvent.class, event -> {
            MessageEmbed embed = Util.embedBuilder("Карта загружена", Colors.purple);
            Bot.sendEmbed(embed);
        });

        Events.on(EventType.GameOverEvent.class, event -> {
            MessageEmbed embed = Util.embedBuilder("Игра окончена", Colors.purple);
            Bot.sendEmbed(embed);
        });

        Events.on(EventType.PlayerJoin.class, event -> {
            String name = StringUtils.stripColorsAndGlyphs(event.player.name);
            MessageEmbed embed = Util.embedBuilder(String.format("%s зашел на сервер", name), Colors.green);
            Bot.sendEmbed(embed);
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            String name = StringUtils.stripColorsAndGlyphs(event.player.name);
            MessageEmbed embed = Util.embedBuilder(String.format("%s покинул сервер", name), Colors.red);
            Bot.sendEmbed(embed);
        });

        Events.on(EventType.PlayerChatEvent.class, event -> {
            if (event.message.startsWith("/")) {
                return;
            }
            String name = StringUtils.stripColorsAndGlyphs(event.player.name);
            String text = StringUtils.stripColorsAndGlyphs(event.message);
            String msg = String.format("**%s**: %s", name, text);
            Bot.sendMessage(msg);
        });
    }
}
