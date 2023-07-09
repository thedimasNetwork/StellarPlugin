package stellar.plugin.util;

import arc.util.Log;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import mindustry.gen.Player;
import stellar.plugin.Const;
import stellar.plugin.database.Database;
import stellar.plugin.database.gen.Tables;
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

    public static String translateChat(Player player, Player otherPlayer, String message) {
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
        if (!otherPlayer.locale.equals(player.locale()) && !"off".equals(locale)) {
            try {
                String targetLocale = "auto".equals(locale) || "double".equals(locale) ? otherPlayer.locale : locale;
                translated = Translator.translate(message, targetLocale.split("#")[0], "auto");
            } catch (Throwable t) {
                Log.err(t);
            }
        }

        String prefix = player.admin() ? "\uE82C" : "\uE872";
        String playerName = player.coloredName();

        return MessageFormat.format("double".equals(locale) ? Const.CHAT_FORMAT_DETAILED : Const.CHAT_FORMAT,
                prefix, playerName, translated, message);
    }

}
