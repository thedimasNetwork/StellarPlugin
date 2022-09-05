package stellar.bot;

import mindustry.gen.Call;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import static stellar.Variables.config;

public class DiscordListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!(event.getChannel().getIdLong() == config.bot.channelId)) {
            return;
        }

        if (event.getAuthor().isBot()) {
            return;
        }

        // \uE80D
        String name = event.getAuthor().getName();
        String message = event.getMessage().getContentStripped();
        String format = "<[blue]\uE80D[]> %s: %s";
        Call.sendMessage(String.format(format, name, message));
    }
}
