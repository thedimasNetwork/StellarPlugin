package stellar.plugin.util;

import arc.util.Log;
import arc.util.io.Streams;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import mindustry.gen.Player;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import stellar.database.Database;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.util.logger.DiscordLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

public class Translator {
    private static final JsonReader jsonReader = new JsonReader();

    public static String translate(String text, String langTo, String langFrom) throws IOException {
        /*
         * Второй вариант переводчика. Ответ парсить сложнее
         * Url:  https://translate.googleapis.com/translate_a/single?client=gtx&sl=ru_RU&tl=en_US&dt=t&q=Привет
         * Resp: [[["Hi","Привет",null,null,10]],null,"ru",null,null,null,null,[]]
         */
        String urlStr = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&dt=t" +
                "&tl=" + langTo +
                "&sl=" + langFrom + // NOTE: use "&sl=auto" for automatic translations
                "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

        Request request = new Request.Builder()
                .url(urlStr)
                .build();
        try (Response response = Variables.httpClient.newCall(request).execute()) {
            JsonValue json = jsonReader.parse(response.body().string());
            return json.get(0).get(0).asString();
        } catch (Throwable t) {
            throw new IOException("Failed to get translation.", t);
        }
    }

    public static String translateRaw(Player player, Player otherPlayer, String message) {
        String locale = otherPlayer.locale;
        try {
            locale = Database.getPlayer(player.uuid()).getTranslator();
        } catch (Throwable t) {
            Log.err(t);
            DiscordLogger.err(t);
        }

        String translated = message;
        if (!otherPlayer.locale.equals(player.locale()) && !locale.equals("off")) {
            try {
                String targetLocale = locale.equals("auto") || locale.equals("double") ? otherPlayer.locale : locale;
                translated = Translator.translate(message, targetLocale.split("#")[0], "auto");
            } catch (Throwable t) {
                Log.err(t);
            }
        }

        return translated;
    }

    public static String formatChat(Player player, String translated, String message, boolean detailed) {
        return MessageFormat.format(detailed ? Const.chatFormatDetailed : Const.chatFormat, Players.prefixName(player), translated, message);
    }


    public static String translateChat(Player player, Player otherPlayer, String message) {
        String locale = otherPlayer.locale;
        try {
            locale = Database.getPlayer(otherPlayer.uuid()).getTranslator();
        } catch (Throwable t) {
            Log.err(t);
            DiscordLogger.err(t);
        }

        return Translator.formatChat(player, Translator.translateRaw(player, otherPlayer, message), message, locale.equals("double"));
    }
}
