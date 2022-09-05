package stellar.bot;

import arc.util.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.entities.EntityBuilder;

import javax.security.auth.login.LoginException;

import java.util.Collections;

import static stellar.Variables.config;

public class Bot {
    private static JDA jda;
    private static TextChannel channel;
    public static void load() {
        try {
            Activity activity = null;
            if (config.bot.main) {
                activity = EntityBuilder.createActivity("Mindustry на play.thedimas.pp.ua", null, Activity.ActivityType.PLAYING);
            }
            jda = JDABuilder.createDefault(config.bot.token) // don't use the deprecated constructor
                    .setActivity(activity)
                    .addEventListeners(new DiscordListener()) // register your listener
                    .build()
                    .awaitReady();
            channel = jda.getTextChannelById(config.bot.channelId);
        } catch (InterruptedException | LoginException e) {
            Log.err(e);
        }
        ServerListener.listen();
    }

    public static void shutdown() {
        jda.shutdownNow();
    }

    public static void sendEmbed(MessageEmbed embed) {
        channel.sendMessageEmbeds(embed).queue();
    }

    public static void sendMessage(String content) {
        MessageCreateData message = new MessageCreateBuilder()
                .setContent(content)
                .setAllowedMentions(Collections.emptyList())
                .build();
        channel.sendMessage(message).queue();
    }
}
