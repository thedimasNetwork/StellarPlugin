package stellar.plugin.util;

import arc.util.Log;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import mindustry.gen.Player;
import stellar.plugin.Const;
import stellar.database.Database;
import stellar.database.gen.Tables;
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
        // Второй вариант переводчика. Ответ парсить сложнее
        // * Url:  https://translate.googleapis.com/translate_a/single?client=gtx&sl=ru_RU&tl=en_US&dt=t&q=Привет
        // * Resp: [[["Hi","Привет",null,null,10]],null,"ru",null,null,null,null,[]]
        String urlStr = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&dt=t" +
                "&tl=" + langTo +
                "&sl=" + langFrom + // use "&sl=auto" for automatic translations
                "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

        URL url = new URL(urlStr);
        StringBuilder response = new StringBuilder();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        Log.debug(response.toString());
        JsonValue json = jsonReader.parse(response.toString());
        return json.get(0).get(0).asString();
    }

    public static String translateRaw(Player player, Player otherPlayer, String message) {
        String locale = otherPlayer.locale;
        try {
            locale = Database.getContext()
                    .select(Tables.USERS.TRANSLATOR)
                    .from(Tables.USERS)
                    .where(Tables.USERS.UUID.eq(otherPlayer.uuid()))
                    .fetchOne().value1();
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
        return MessageFormat.format(detailed ? Const.CHAT_FORMAT_DETAILED : Const.CHAT_FORMAT, Players.prefixName(player), translated, message);
    }


    public static String translateChat(Player player, Player otherPlayer, String message) {
        String locale = otherPlayer.locale;
        try {
            locale = Database.getPlayer(player.uuid()).getTranslator();
        } catch (Throwable t) {
            Log.err(t);
            DiscordLogger.err(t);
        }

        return Translator.formatChat(player, Translator.translateRaw(player, otherPlayer, message), message, locale.equals("double"));
    }
}
