package stellar.plugin.util.logger;

import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.net.Administration;
import webhook.Webhook;
import webhook.embed.Embed;
import webhook.http.Part;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static stellar.plugin.Variables.config;

@SuppressWarnings("unused")
public class DiscordLogger {
    private static ObjectMap<LogLevel, Log.LogLevel> levels = ObjectMap.of(
            LogLevel.debug, Log.LogLevel.debug,
            LogLevel.info, Log.LogLevel.info,
            LogLevel.warn, Log.LogLevel.warn,
            LogLevel.err, Log.LogLevel.err
    );

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

        if (th == null) {
            Log.log(levels.get(level), text);
        } else {
            Webhook.sendMultipart(config.webhookUrl, Part.ofBytes("file0", "text/plain",
                    Strings.getStackTrace(th).getBytes(StandardCharsets.UTF_8),
                    "StackTrace" + Instant.now().toEpochMilli() + ".txt"));
            if (text != null) {
                Log.log(levels.get(level), text + ": " + Strings.getStackTrace(th));
            } else {
                Log.log(levels.get(level), Strings.getStackTrace(th));
            }
        }
    }

    public static void info(String text) {
        log(LogLevel.info, text);
    }

    public static void infoTag(String tag, String text) {
        log(LogLevel.info, "[" + tag + "] " + text);
    }

    public static void warn(String text) {
        log(LogLevel.warn, text);
    }

    public static void warnTag(String tag, String text) {
        log(LogLevel.warn, "[" + tag + "] " + text);
    }

    public static void err(String text) {
        log(LogLevel.err, text);
    }

    public static void err(Throwable th) {
        log(LogLevel.err, null, th);
    }

    public static void err(String text, Throwable th) {
        log(LogLevel.err, text, th);
    }

    public static void errTag(String tag, String text) {
        log(LogLevel.err, "[" + tag + "] " + text);
    }

    public static void debug(String text) {
        log(LogLevel.debug, text);
    }

    public static void debugTag(String tag, String text) {
        log(LogLevel.debug, "[" + tag + "] " + text);
    }
}
