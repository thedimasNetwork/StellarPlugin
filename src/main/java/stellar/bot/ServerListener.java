package stellar.bot;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import stellar.ThedimasPlugin;
import stellar.util.StringUtils;

public class ServerListener {
    public static void listen() {
        Events.on(EventType.ServerLoadEvent.class, event -> {
            MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Сервер запущен")
                    .setColor(Colors.green)
                    .build();
            Bot.sendEmbed(embed);
        });

        Events.on(EventType.WorldLoadEvent.class, event -> {
            MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Карта загружена")
                    .setColor(Colors.purple)
                    .build();
            Bot.sendEmbed(embed);
        });

        Events.on(EventType.GameOverEvent.class, event -> {
            MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Игра окончена")
                    .setColor(Colors.purple)
                    .build();
            Bot.sendEmbed(embed);
        });

        Events.on(EventType.PlayerJoin.class, event -> {
            String name = StringUtils.stripColorsAndGlyphs(event.player.name);
            MessageEmbed embed = new EmbedBuilder()
                    .setDescription(String.format("%s зашел на сервер", name))
                    .setColor(Colors.green)
                    .build();
            Bot.sendEmbed(embed);
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            String name = StringUtils.stripColorsAndGlyphs(event.player.name);
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
            String name = StringUtils.stripColorsAndGlyphs(event.player.name);
            String text = StringUtils.stripColorsAndGlyphs(event.message);
            String msg = String.format("**%s**: %s", name, text);
            Bot.sendMessage(msg);
        });
    }
}
