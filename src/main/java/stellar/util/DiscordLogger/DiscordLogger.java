package stellar.util.DiscordLogger;

import arc.util.Log;
import stellar.Const;
import webhook.Webhook;
import webhook.embed.Embed;

import java.io.*;
import java.time.Instant;

@SuppressWarnings("unused")
public class DiscordLogger {

    public static void log(LogLevel level, String text) {
        log(level, text, null);
    }

    public static void log(LogLevel level, String text, File file) {
        Embed embed = new Embed();
        if (text != null) {
            embed.addField(level.name, text);
        } else {
            embed.setTitle(level.name);
        }
        embed.setColor(level.color);

        try {
            new Webhook(Const.WEBHOOK_LOG_URL)
                    .addEmbed(embed)
                    .execute();
            if (file != null) {
                Webhook.sendFile(Const.WEBHOOK_LOG_URL, file);
            }
        } catch (IOException | InterruptedException e) {
            Log.err(e);
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
        err(null, th);
    }

    public static void err(String text, Throwable th) {
        File file = getStackTraceFile(th);
        log(LogLevel.ERR, text, file);
        if (!file.delete()) {
            file.deleteOnExit();
        }
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

    private static String getStackTrace(Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        return sw.toString();
    }

    private static File getStackTraceFile(Throwable th) {
        File file = new File(Const.PLUGIN_FOLDER, "StackTrace" + Instant.now().toEpochMilli() + ".txt");
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(getStackTrace(th));
            writer.flush();
        } catch (IOException e) {
            Log.err(e);
        }
        return file;
    }
}
