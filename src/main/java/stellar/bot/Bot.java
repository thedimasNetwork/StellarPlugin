package stellar.bot;

import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import net.dv8tion.jda.internal.entities.EntityBuilder;

import javax.security.auth.login.LoginException;
import java.util.Collections;
import java.util.EnumSet;

import static stellar.Variables.config;

public class Bot {
    private static JDA jda;
    private static TextChannel channel;

    public static void load() {
        try {
            Activity activity = null;
            if (config.bot.main) {
                activity = EntityBuilder.createActivity("Mindustry на play.thedimas.pp.ua", config.discordUrl, Activity.ActivityType.PLAYING);
            }
            Log.info("Building JDA...");
            jda = JDABuilder.createDefault(config.bot.token) // don't use the deprecated constructor
                    .setActivity(activity)
                    .addEventListeners(new DiscordListener()) // register your listener
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .build()
                    .awaitReady();
            MessageRequest.setDefaultMentions(EnumSet.of(Message.MentionType.CHANNEL, Message.MentionType.EMOJI));
            channel = jda.getTextChannelById(config.bot.channelId);
            Log.info("JDA is ready!");
        } catch (InterruptedException | LoginException e) {
            Log.err(e);
        }
        ServerListener.listen();
        jda.updateCommands().addCommands(
                Commands.slash("info", "Информация про сервер"),
                Commands.slash("players", "Игроки на сервере"),
                Commands.slash("host", "Информация про хост"),
                Commands.slash("maps", "Список карт на сервере"),
                Commands.slash("skipwave", "Пропустить волну"),
                Commands.slash("gameover", "Принудительно завершить игру"),
                Commands.slash("find", "Найти игрока")
                        .addOptions(
                                new OptionData(OptionType.STRING, "type", "Тип информации по которой искать")
                                        .addChoice("Имя", "name")
                                        .addChoice("Айди", "id")
                                        .addChoice("Айпи", "ip")
                                        .addChoice("UUID", "uuid")
                                        .setRequired(true),
                                new OptionData(OptionType.STRING, "query", "Запрос")
                                        .setRequired(true)
                        )

        ).queue();
    }

    public static void shutdown() {
        Log.info("Shutting down bot...");
        jda.shutdown();
    }

    public static void sendEmbed(MessageEmbed embed) {
        new Thread(() -> {
            channel.sendMessageEmbeds(embed).queue();
        }).start();
    }

    public static void sendEmbed(long channelId, MessageEmbed embed) {
        new Thread(() -> {
            jda.getTextChannelById(channelId).sendMessageEmbeds(embed).queue();
        }).start();
    }


    public static void sendMessage(String content) {
        MessageCreateData message = new MessageCreateBuilder()
                .setContent(content)
                .setAllowedMentions(Collections.singleton(Message.MentionType.CHANNEL))
                .build();
        new Thread(() -> {
            channel.sendMessage(message).queue();
        }).start();
    }
}
