package stellar.util.logger;

import arc.util.Nullable;
import arc.util.Strings;
import mindustry.net.Administration;
import webhook.Webhook;
import webhook.embed.Embed;
import webhook.http.Part;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static stellar.Variables.config;

@SuppressWarnings("unused")
public class DiscordLogger {

    public static void log(LogLevel level, String text) {
        log(level, text, null);
    }

    public static void log(LogLevel level, String text, @Nullable Throwable th) {
        Embed embed = new Embed();
        if (text != null) {
            embed.addField(level.name, text);
        } else {
            embed.setTitle(level.name);
        }
        embed.setColor(level.color)
                .setFooter(Administration.Config.serverName.string(), null)
                .setTimestamp(Instant.now().toString());

        new Webhook(config.webhookUrl)
                .addEmbed(embed)
                .execute();

        if (th != null) {
            Webhook.sendMultipart(config.webhookUrl, Part.ofBytes("file0", "text/plain",
                    Strings.getStackTrace(th).getBytes(StandardCharsets.UTF_8),
                    "StackTrace" + Instant.now().toEpochMilli() + ".txt"));
        }
    }

    public static void info(String text) {
        log(LogLevel.INFO, text);
    }

    public static void infoTag(String tag, String text) {
        log(LogLevel.INFO, "[" + tag + "] " + text);
    }

    public static void warn(String text) {
        log(LogLevel.WARN, text);
    }

    public static void warnTag(String tag, String text) {
        log(LogLevel.WARN, "[" + tag + "] " + text);
    }

    public static void err(String text) {
        log(LogLevel.ERR, text);
    }

    public static void err(Throwable th) {
        log(LogLevel.ERR, null, th);
    }

    public static void err(String text, Throwable th) {
        log(LogLevel.ERR, text, th);
    }

    public static void errTag(String tag, String text) {
        log(LogLevel.ERR, "[" + tag + "] " + text);
    }

    public static void debug(String text) {
        log(LogLevel.DEBUG, text);
    }

    public static void debugTag(String tag, String text) {
        log(LogLevel.DEBUG, "[" + tag + "] " + text);
    }
}
