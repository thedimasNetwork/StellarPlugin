package stellar.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.LocalDateTime;

public class Util {
    public static MessageEmbed embedBuilder(String text, Color color) {
        return new EmbedBuilder()
                .setDescription(text)
                .setColor(color)
                .build();
    }
    public static MessageEmbed embedBuilder(String text, Color color, LocalDateTime time) {
        return new EmbedBuilder()
                .setDescription(text)
                .setColor(color)
                .setTimestamp(time)
                .build();
    }
    public static MessageEmbed embedBuilder(String title, String description, Color color, LocalDateTime time) {
        return new EmbedBuilder()
                .addField(title, description, false)
                .setColor(color)
                .setTimestamp(time)
                .build();
    }
}
